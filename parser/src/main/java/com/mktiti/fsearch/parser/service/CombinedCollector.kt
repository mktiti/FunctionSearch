package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.repo.TypeResolver

interface CombinedCollector<I> {

    data class Result(
            val typeRepo: TypeRepo,
            val functions: Collection<FunctionObj>
    )

    fun collectCombined(info: I, dependencyResolver: TypeResolver): Result

}
