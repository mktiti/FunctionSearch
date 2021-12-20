package com.mktiti.fsearch.grpc.converter

import com.mktiti.fsearch.dto.SearchLog
import com.mktiti.fsearch.dto.SearchStatistics
import com.mktiti.fsearch.dto.UserInfo
import com.mktiti.fsearch.dto.UserLevel
import com.mktiti.fsearch.dto.UserLevel.NORMAL
import com.mktiti.fsearch.dto.UserLevel.PREMIUM
import com.mktiti.fsearch.grpc.Admin
import com.mktiti.fsearch.grpc.User

internal fun UserLevel.toProto() = when (this) {
    NORMAL -> User.UserLevel.NORMAL
    PREMIUM -> User.UserLevel.PREMIUM
}

fun UserInfo.toProto(): User.UserInfo = User.UserInfo.newBuilder()
        .setLevel(level.toProto())
        .setRegisterDate(registerDate)
        .addAllSavedContexts(savedContexts.map { it.toProto() })
        .build()

fun SearchLog.toProto(): Admin.SearchLog = Admin.SearchLog.newBuilder()
        .setRequest(request.toProto())
        .setResult(result)
        .build()

fun SearchStatistics.toProto(): Admin.SearchStatistics = Admin.SearchStatistics.newBuilder()
        .setNumberOfUsers(numberOfUsers)
        .addAllLastSearches(lastSearches.map { it.toProto() })
        .build()