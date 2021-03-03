package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult

internal fun <T> nopResult(): ApiCallResult<T> = ApiCallResult.Exception(-1, "Service not set")