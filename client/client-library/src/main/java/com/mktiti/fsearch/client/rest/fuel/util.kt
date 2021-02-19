package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory.Convenience
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import com.mktiti.fsearch.client.rest.ApiCallResult

private typealias FuelResp<T> = Triple<Request, Response, Result<T, FuelError>>
private typealias FuelConverter<T> = Request.() -> FuelResp<T>

internal inline fun <reified T> Convenience.getJson(path: String, params: Parameters = emptyList()): ApiCallResult<T> {
    return getJsonConv(path, params) {
        responseObject()
    }
}

private fun <T> Convenience.getJsonConv(
        path: String,
        params: Parameters,
        converter: FuelConverter<T>
): ApiCallResult<T> {
    val (_, resp, result) = get(path, params).converter()

    return when (result) {
        is Result.Success -> ApiCallResult.Success(result.value)
        is Result.Failure -> ApiCallResult.Exception(resp.statusCode, result.getException().message ?: "Unknown error")
    }
}

internal inline fun <reified T> Convenience.postJson(path: String, body: Any): ApiCallResult<T> {
    return postJsonConv(path, body) {
        responseObject()
    }
}

private fun <T> Convenience.postJsonConv(path: String, body: Any, converter: FuelConverter<T>): ApiCallResult<T> {
    val (_, resp, result) = post(path)
            .header("Content-Type" to "application/json")
            .objectBody(body)
            .converter()

    return when (result) {
        is Result.Success -> ApiCallResult.Success(result.value)
        is Result.Failure -> ApiCallResult.Exception(resp.statusCode, result.getException().message ?: "Unknown error")
    }
}

internal fun Convenience.postUnit(path: String, body: Any): ApiCallResult<Unit> {
    val (_, resp, result) = post(path)
            .header("Content-Type" to "application/json")
            .objectBody(body)
            .response()

    return when (result) {
        is Result.Success -> ApiCallResult.Success(Unit)
        is Result.Failure -> ApiCallResult.Exception(resp.statusCode, result.getException().message ?: "Unknown error")
    }
}

internal fun Convenience.deleteBoolean(path: String): ApiCallResult<Boolean> {
    val (_, resp, result) = delete(path).responseObject<Boolean>()

    return when (result) {
        is Result.Success -> ApiCallResult.Success(result.value)
        is Result.Failure -> ApiCallResult.Exception(resp.statusCode, result.getException().message ?: "Unknown error")
    }
}
