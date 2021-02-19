package com.mktiti.fsearch.client.rest

sealed class ApiCallResult<T> {
    data class Success<T>(val result: T) : ApiCallResult<T>()

    data class Exception<T>(
            val code: Int,
            val message: String
    ) : ApiCallResult<T>()
}
