package com.mktiti.fsearch.backend.stats

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.model.build.intermediate.QueryImports

sealed interface SearchResult {
    object QueryError : SearchResult
    object SearchError : SearchResult

    data class Ok(val results: String) : SearchResult
}

data class SearchLog(
        val context: ContextId,
        val imports: QueryImports,
        val query: String,
        val result: SearchResult
)

data class SearchStatistics(
        val latestSearches: List<SearchLog>
)