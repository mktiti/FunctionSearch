package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.QueryCtxDto
import com.mktiti.fsearch.backend.api.QueryResult
import com.mktiti.fsearch.backend.api.SearchHandler
import com.mktiti.fsearch.backend.api.TypeHint
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.util.TypeException
import org.antlr.v4.runtime.misc.ParseCancellationException

class BasicSearchHandler(
        private val contextManager: ContextManager,
        private val fitPresenter: FitPresenter
) : SearchHandler {

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

            val results = fitter.findFittings(parsedQuery, domain.functions.parallelStream()).map { (function, fit) ->
                fitPresenter.present(function, fit, docStore.getOrEmpty(function.info))
            }

            QueryResult.Success(query, results)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        QueryResult.Error.InternalError(query, "Internal error, see logs for details")
    }

}