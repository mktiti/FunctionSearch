package com.mktiti.fsearch.client.cli.util

import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.dto.QueryCtxDto

fun Context.contextDto() = QueryCtxDto(
        artifacts = artifacts.toList()
)
