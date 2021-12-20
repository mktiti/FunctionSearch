package com.mktiti.fsearch.grpc.converter

import com.mktiti.fsearch.dto.SearchLog
import com.mktiti.fsearch.dto.SearchStatistics
import com.mktiti.fsearch.dto.UserInfo
import com.mktiti.fsearch.dto.UserLevel.NORMAL
import com.mktiti.fsearch.dto.UserLevel.PREMIUM
import com.mktiti.fsearch.grpc.Admin
import com.mktiti.fsearch.grpc.User

internal fun User.UserLevel.toDto() = when (this) {
    User.UserLevel.NORMAL -> NORMAL
    User.UserLevel.PREMIUM -> PREMIUM
}

fun User.UserInfo.toDto() = UserInfo(
        level = level.toDto(),
        registerDate = registerDate,
        savedContexts = savedContextsList.map { it.toDto() }
)

fun Admin.SearchLog.toDto() = SearchLog(
        request = request.toDto(),
        result = result
)

fun Admin.SearchStatistics.toDto() = SearchStatistics(
        numberOfUsers = numberOfUsers,
        lastSearches = lastSearchesList.map { it.toDto() }
)