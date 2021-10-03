package com.mktiti.fsearch.parser.connect

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.util.InfoMap

data class JclTypeResult(val javaRepo: JavaRepo, val jclRepo: TypeRepo)

data class FunctionCollection(
        val staticFunctions: Collection<FunctionObj>,
        val instanceMethods: InfoMap<Collection<FunctionObj>>
) {

    fun asPair(): Pair<Collection<FunctionObj>, InfoMap<Collection<FunctionObj>>> = staticFunctions to instanceMethods

}