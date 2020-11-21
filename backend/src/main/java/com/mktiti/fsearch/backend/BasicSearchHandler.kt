package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.*
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.FallbackResolver
import com.mktiti.fsearch.core.util.TypeException
import kotlin.streams.toList

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
            val queryRes = try {
                queryParser.parse(query)
            } catch (te: TypeException) {
                return@with QueryResult.Error.Query(query, "Failed to parse query - ${te.message}")
            }

            val resolver = FallbackResolver.withVirtuals(queryRes.virtualTypes, domain.typeResolver)
            val fitter = JavaQueryFitter(infoRepo, resolver)

            val results = domain.functions.parallelStream().map { function ->
                fitter.fitsQuery(queryRes.query, function)?.let {
                    function to it
                }
            }.toList().filterNotNull().map { (function, fit) ->
                fitPresenter.present(function, fit, docStore.getOrEmpty(function.info))
            }

            QueryResult.Success(query, results)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        QueryResult.Error.InternalError(query, "Internal error, see logs for details")
    }

}