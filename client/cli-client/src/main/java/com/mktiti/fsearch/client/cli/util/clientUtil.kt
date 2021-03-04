package com.mktiti.fsearch.client.cli.util

import com.mktiti.fsearch.client.cli.job.JobPrinter
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.Service

fun runHealthCheck(service: Service, printer: JobPrinter): Boolean {
    printer.print("Running API health check... ")
    return when (val checkRes = service.searchApi.healthCheck()) {
        is ApiCallResult.Success -> {
            printer.println(checkRes.result.message)
            true
        }
        is ApiCallResult.Exception -> {
            printer.println("[${checkRes.code}] - ${checkRes.message}")
            false
        }
    }
}
