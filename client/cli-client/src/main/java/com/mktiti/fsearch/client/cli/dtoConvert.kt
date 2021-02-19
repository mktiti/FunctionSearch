package com.mktiti.fsearch.client.cli

import com.mktiti.fsearch.dto.QueryCtxDto

fun Context.contextDto() = QueryCtxDto(
        artifacts = artifacts.toList()
)
