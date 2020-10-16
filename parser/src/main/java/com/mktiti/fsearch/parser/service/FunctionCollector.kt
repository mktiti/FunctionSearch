package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeResolver

interface FunctionCollector<I> {

    fun collectFunctions(info: I, dependencyResolver: TypeResolver): Collection<FunctionObj>

}
