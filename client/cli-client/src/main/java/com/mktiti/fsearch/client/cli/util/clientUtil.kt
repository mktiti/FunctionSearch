package com.mktiti.fsearch.client.cli.util
/*
import io.swagger.client.infrastructure.ClientException
import io.swagger.client.infrastructure.ServerException

sealed class ApiCallResult<T> {
    data class Success<T>(val result: T) : ApiCallResult<T>()

    sealed class Exception<T>(
            val message: String
    ) : ApiCallResult<T>() {

        constructor(exception: java.lang.Exception) : this(exception.message ?: "Unknown error")

        data class Client<T>(val exception: ClientException) : Exception<T>(exception)
        data class Server<T>(val exception: ServerException) : Exception<T>(exception)
    }
}

inline fun <T> safeApiCall(call: () -> T): ApiCallResult<T> {
    return try {
        val result = call()
        ApiCallResult.Success(result)
    } catch (clientException: ClientException) {
        ApiCallResult.Exception.Client(clientException)
    } catch (serverException: ServerException) {
        ApiCallResult.Exception.Server(serverException)
    }
}
*/