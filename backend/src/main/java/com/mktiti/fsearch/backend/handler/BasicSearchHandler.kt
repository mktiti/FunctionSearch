package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.search.SearchService
import com.mktiti.fsearch.backend.search.SearchService.ContextStatus
import com.mktiti.fsearch.backend.stats.SearchLog
import com.mktiti.fsearch.backend.stats.SearchResult
import com.mktiti.fsearch.backend.stats.StatisticService
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.rest.api.handler.SearchHandler
import com.mktiti.fsearch.util.logDebug
import com.mktiti.fsearch.util.logError
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import org.antlr.v4.runtime.misc.ParseCancellationException

class BasicSearchHandler(
        private val searchService: SearchService,
        private val fitPresenter: FitPresenter,
        private val statService: StatisticService,
        private val resultLimit: Int = 50
) : SearchHandler {

    companion object {
        private val okHealth = HealthInfo(
                version = ProjectInfo.version,
                buildTimestamp = ProjectInfo.builtAt,
                ok = true
        )
    }

    private val log = logger()

    override fun healthCheck(): HealthInfo = okHealth

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

    private fun okLogEntry(results: ResultList<QueryFitResult>) = SearchResult.Ok(
            results.results.joinToString(prefix = "[", postfix = ",", separator = ", ", transform = QueryFitResult::header)
    )

    override fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult {
        log.logInfo { "Searching '$query' (context: $contextId)" }

        val artifacts = contextId.artifactsId()
        val imports = contextId.imports()

        fun addToStats(result: SearchResult) {
            statService += SearchLog(artifacts, imports, query, result)
        }

        fun queryError(e: Exception): QueryResult.Error {
            log.logInfo(e) { "Failed to parse query '$query'" }
            addToStats(SearchResult.QueryError)
            return QueryResult.Error.Query(query, "Failed to parse query - ${e.message}")
        }

        return try {
            val results = searchService.search(artifacts, imports, query)
                    .map { (function, fit, doc) ->
                        fitPresenter.present(function, fit, doc)
                    }.limitedResult(resultLimit)

            log.logDebug { "Search success '$query' -> $results" }
            addToStats(okLogEntry(results))
            QueryResult.Success(query, results)
        } catch (te: TypeException) {
            queryError(te)
        } catch (pe: ParseCancellationException) {
            queryError(pe)
        } catch (e: Exception) {
            log.logError(e) { "Failed to execute (synchronous) query '$query'" }
            addToStats(SearchResult.SearchError)
            QueryResult.Error.InternalError(query, "Internal error, see logs for details")
        }
    }

}