package com.mktiti.fsearch.client.cli.util

import com.mktiti.fsearch.client.cli.job.BackgroundJobContext
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.dto.ResultList

fun <T> BackgroundJobContext.onResults(callResult: ApiCallResult.Success<ResultList<T>>, onItem: (T) -> Unit) {
    onResults(callResult.result, onItem)
}

fun <T> BackgroundJobContext.onResults(result: ResultList<T>, onItem: (T) -> Unit) {
    result.results.forEach(onItem)
    if (result.trimmed) {
        printer.println("--- Shown results limited ---")
    }
}