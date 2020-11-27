package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.*
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.util.TypeException
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.util.concurrent.Executors
import kotlin.streams.toList

class BasicSearchHandler(
        private val contextManager: ContextManager,
        private val fitPresenter: FitPresenter
) : SearchHandler {

    private val service = Executors.newCachedThreadPool()

    override fun typeHint(contextId: QueryCtxDto, namePart: String): List<TypeHint> {
        return with(contextManager[contextId.toId()]) {
            domain.typeResolver.allSemis().filter {
                with(it.info) {
                    simpleName.startsWith(namePart, true) || fullName.startsWith(namePart, true)
                }
            }.take(50).map { semi ->
                TypeHint(
                        file = semi.info.fullName,
                        typeParamCount = semi.typeParamCount
                )
            }.toList()
        }
    }

    // TODO primitive
    override fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus {
        val id = contextId.toId()
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
        with(contextManager[contextId.toId()]) {
            val (parsedQuery, virtuals) = try {
                queryParser.parse(query)
            } catch (te: TypeException) {
                return@with QueryResult.Error.Query(query, "Failed to parse query - ${te.message}")
            } catch (pe: ParseCancellationException) {
                return@with QueryResult.Error.Query(query, "Failed to parse query - ${pe.message}")
            }

            val resolver = FallbackResolver.withVirtuals(virtuals, domain.typeResolver)
            val fitter = JavaQueryFitter(infoRepo, resolver)

            val results = fitter.findFittings(parsedQuery, domain.staticFunctions, domain.instanceFunctions).map { (function, fit) ->
                fitPresenter.present(function, fit, docStore.getOrEmpty(function.info))
            }.limit(50).toList()

            QueryResult.Success(query, results)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        QueryResult.Error.InternalError(query, "Internal error, see logs for details")
    }

}