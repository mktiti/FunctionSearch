package com.mktiti.fsearch.core.javadoc

import com.mktiti.fsearch.core.fit.FunctionInfo

interface FunDocResolver {

    companion object {
        fun nop(): FunDocResolver = SingleDocMapStore(emptyMap())
    }

    operator fun get(id: FunctionInfo): FunctionDoc?

    fun getOrEmpty(id: FunctionInfo): FunctionDoc = get(id) ?: FunctionDoc()

}

class SingleDocMapStore(
        private val store: Map<FunctionInfo, FunctionDoc>
) : FunDocResolver {

    override fun get(id: FunctionInfo) = store[id]

}

class SimpleMultiDocStore(
        private val resolvers: Collection<FunDocResolver>
) : FunDocResolver {

    override fun get(id: FunctionInfo) = resolvers.asSequence().mapNotNull {
        it[id]
    }.firstOrNull()

}
