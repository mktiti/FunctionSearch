package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.TypeRepo

interface FunctionCollector<I> {

    fun collectFunctions(info: I, depsRepos: Collection<TypeRepo>): Collection<FunctionObj>

}
