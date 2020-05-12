package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeRepo

interface CombinedCollector<I> {

    data class Result(
            val typeRepo: TypeRepo,
            val functions: Collection<FunctionObj>
    )

    fun collectCombined(info: I, depsRepo: Collection<TypeRepo>): Result

}
