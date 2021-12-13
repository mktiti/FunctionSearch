package com.mktiti.fsearch.backend.handler.basic

import com.mktiti.fsearch.backend.handler.SearchHandler
import com.mktiti.fsearch.backend.search.SearchService
import com.mktiti.fsearch.backend.search.SearchService.ContextStatus
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.util.logDebug
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import org.antlr.v4.runtime.misc.ParseCancellationException

class BasicSearchHandler(
        private val searchService: SearchService,
        private val fitPresenter: FitPresenter,
        private val resultLimit: Int = 50
) : SearchHandler {

    private val log = logger()

    override fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint> {
        return searchService.typeHint(contextId.artifactsId(), namePart)
                .map { semi ->
                    TypeHint(
                            file = semi.info.fullName,
                            typeParamCount = semi.typeParamCount
                    )
                }.limitedResult(resultLimit)
    }

    // TODO primitive
    override fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus {
        return when (searchService.preloadContext(contextId.artifactsId())) {
            ContextStatus.LOADED -> ContextLoadStatus.LOADED
            ContextStatus.NOT_LOADED -> ContextLoadStatus.ERROR
        }
    }

    override fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult {
        fun queryError(e: Exception): QueryResult.Error {
            log.logInfo(e) { "Failed to parse query '$query'" }
            return QueryResult.Error.Query(query, "Failed to parse query - ${e.message}")
        }

        log.logInfo { "Searching '$query' (context: $contextId)" }

        return try {
            val results = searchService.search(contextId.artifactsId(), contextId.imports(), query)
                    .map { (function, fit, doc) ->
                        fitPresenter.present(function, fit, doc)
                    }.limitedResult(resultLimit)

            log.logDebug { "Search success '$query' -> $results" }
            QueryResult.Success(query, results)
        } catch (te: TypeException) {
            return queryError(te)
        } catch (pe: ParseCancellationException) {
            return queryError(pe)
        } catch (e: Exception) {
            log.logError(e) { "Failed to execute (synchronous) query '$query'" }
            QueryResult.Error.InternalError(query, "Internal error, see logs for details")
        }
    }

}