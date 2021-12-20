package com.mktiti.fsearch.backend.stats

import org.apache.commons.collections4.QueueUtils
import org.apache.commons.collections4.queue.CircularFifoQueue
import java.util.*

interface StatisticService {

    operator fun plusAssign(log: SearchLog) = logSearch(log)

    fun logSearch(log: SearchLog)

    fun searchStats(): SearchStatistics

}

class InMemStatService(
        searchLogLimit: Int
) : StatisticService {

    private val latestSearches: Queue<SearchLog> = QueueUtils.synchronizedQueue(
            CircularFifoQueue(searchLogLimit)
    )

    override fun logSearch(log: SearchLog) {
        latestSearches.add(log)
    }

    override fun searchStats() = SearchStatistics(
            latestSearches = latestSearches.toList()
    )

}