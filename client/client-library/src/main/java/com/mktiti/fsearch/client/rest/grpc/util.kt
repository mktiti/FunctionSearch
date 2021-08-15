package com.mktiti.fsearch.client.rest.grpc

import com.mktiti.fsearch.client.rest.ApiCallResult

internal fun <T> handleGrpc(code: () -> T): ApiCallResult<T> {
    return try {
        val result = code()
        ApiCallResult.Success(result)
    } catch (exception: Exception) {
        ApiCallResult.Exception(-2, exception.message ?: "Unknown error")
    }
}