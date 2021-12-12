package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.*
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.util.logDebug
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.util.concurrent.Executors
import kotlin.streams.asSequence

class BasicSearchHandler(
        private val resultLimit: Int = 50,
        private val contextManager: ContextManager,
        private val fitPresenter: FitPresenter
) : SearchHandler {

    private val service = Executors.newCachedThreadPool()

    private val log = logger()

    override fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint> {
        return with(contextManager[contextId.artifactsId()]) {
            domain.typeResolver.allSemis().filter {
                with(it.info) {
                    simpleName.startsWith(namePart, true) || fullName.startsWith(namePart, true)
                }
            }.map { semi ->
                TypeHint(
                        file = semi.info.fullName,
                        typeParamCount = semi.typeParamCount
                )
            }.limitedResult(resultLimit)
        }
    }

    // TODO primitive
    override fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus {
        val id = contextId.artifactsId()
        return if (id in contextManager) {
            ContextLoadStatus.LOADED
        } else {
            service.submit {
                contextManager[id]
            }
            ContextLoadStatus.LOADING
        }
    }

    override fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult = try {
        fun queryError(e: Exception): QueryResult.Error {
            log.logInfo(e) { "Failed to parse query '$query'" }
            return QueryResult.Error.Query(query, "Failed to parse query - ${e.message}")
        }

        log.logInfo { "Searching '$query' (context: $contextId)" }
        with(contextManager[contextId.artifactsId()]) {
            val (parsedQuery, virtuals) = try {
                queryParser.parse(query, contextId.imports())
            } catch (te: TypeException) {
                return@with queryError(te)
            } catch (pe: ParseCancellationException) {
                return@with queryError(pe)
            }

            val resolver = FallbackResolver.withVirtuals(virtuals, domain.typeResolver)
            val fitter = JavaQueryFitter(infoRepo, resolver)

            val results = fitter.findFittings(parsedQuery, domain.staticFunctions, domain.instanceFunctions)
                    .asSequence()
                    .map{ (function, fit) ->
                        fitPresenter.present(function, fit, docResolver.getOrEmpty(function.info))
                    }.limitedResult(resultLimit)

            log.logDebug { "Search success '$query' -> $results" }
            QueryResult.Success(query, results)
        }
    } catch (e: Exception) {
        log.logError(e) { "Failed to execute (synchronous) query '$query'" }
        QueryResult.Error.InternalError(query, "Internal error, see logs for details")
    }

}