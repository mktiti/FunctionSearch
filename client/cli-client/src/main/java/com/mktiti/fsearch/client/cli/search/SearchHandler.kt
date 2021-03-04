package com.mktiti.fsearch.client.cli.search

import com.mktiti.fsearch.client.cli.context.Context
import com.mktiti.fsearch.client.cli.job.BackgroundJob
import com.mktiti.fsearch.client.cli.job.voidBackgroundJob
import com.mktiti.fsearch.client.cli.util.contextDto
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.dto.QueryRequestDto
import com.mktiti.fsearch.dto.QueryResult

class SearchHandler {

    companion object {
        private fun constructDto(context: Context, query: String) = QueryRequestDto(
                context = context.contextDto(),
                query = query
        )
    }

    fun searchJob(query: String): BackgroundJob {
        return voidBackgroundJob {
            printer.print("Searching... ")

            val callRes = searchApi.search(constructDto(context, query))
            if (!isCancelled) {
                when (callRes) {
                    is ApiCallResult.Success<QueryResult> -> {
                        when (val queryRes = callRes.result) {
                            is QueryResult.Success -> {
                                printer.println("done!")
                                printer.println("Matching functions:")
                                queryRes.results.forEach {
                                    if (!isCancelled) {
                                        printer.println("========")
                                        printer.println(it.file)
                                        printer.println("\t${it.header}")
                                        it.doc.shortInfo?.let { info ->
                                            printer.println(info)
                                        }
                                    }
                                }
                            }
                            is QueryResult.Error.InternalError -> {
                                printer.println("Error while processing query - ${queryRes.message}")
                            }
                            is QueryResult.Error.Query -> {
                                printer.println("Invalid query - ${queryRes.message}")
                            }
                        }
                    }
                    is ApiCallResult.Exception<QueryResult> -> {
                        printer.println("Failed to search functions")
                        printer.println(callRes.message)
                    }
                }
            }


            /*
            val result = safeApiCall {
                searchApi.search(constructDto(context, query))
            }

            if (!isCancelled) {
                when (result) {
                    is ApiCallResult.Success -> {
                        when (val queryRes = result.result) {
                            is QueryResult.Success -> {
                                queryRes.results.forEach {
                                    printer.println("Search done!")
                                    printer.println("Fitting functions:")
                                    if (isCancelled) {
                                        printer.println(it.file)
                                        printer.println("\t${it.header}")
                                    }
                                }
                            }
                            is QueryResult.Error.InternalError -> {
                                printer.println("Error while processing query - ${queryRes.message}")
                            }
                            is QueryResult.Error.Query -> {
                                printer.println("Invalid query - ${queryRes.message}")
                            }
                        }
                    }
                    is ApiCallResult.Exception -> {
                        printer.println("Failed to search functions")
                        printer.println(result.message)
                    }
                }
            }
             */
        }
    }

}