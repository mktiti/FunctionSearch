package com.mktiti.fsearch.dto

data class SearchLog(
        val request: QueryRequestDto,
        val result: String
)

// Mostly for testing
data class SearchStatistics(
        val numberOfUsers: Long,
        val lastSearches: List<SearchLog>
)