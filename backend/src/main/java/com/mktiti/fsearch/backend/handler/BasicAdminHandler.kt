package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.stats.StatisticService
import com.mktiti.fsearch.backend.user.UserService
import com.mktiti.fsearch.dto.SearchStatistics
import com.mktiti.fsearch.rest.api.handler.AdminHandler

class BasicAdminHandler(
        private val statService: StatisticService,
        private val userService: UserService
) : AdminHandler {

    override fun searchStats(): SearchStatistics {
        val latestSearches = statService.searchStats().latestSearches
        val userCount = userService.userCount()

        return SearchStatistics(userCount, latestSearches.toDto())
    }

}