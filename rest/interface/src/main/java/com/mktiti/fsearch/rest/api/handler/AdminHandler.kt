package com.mktiti.fsearch.rest.api.handler

import com.mktiti.fsearch.dto.SearchStatistics

interface AdminHandler {

    object Nop : AdminHandler {
        override fun searchStats(): SearchStatistics = SearchStatistics(0, emptyList())
    }

    fun searchStats(): SearchStatistics

}
