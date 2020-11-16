package com.mktiti.fsearch.core.javadoc

import com.mktiti.fsearch.core.fit.FunctionInfo

interface DocStore {

    companion object {
        fun nop(): DocStore = SingleDocMapStore(emptyMap())
    }

    operator fun get(id: FunctionInfo): FunctionDoc?

    fun getOrEmpty(id: FunctionInfo): FunctionDoc = get(id) ?: FunctionDoc()

}

class SingleDocMapStore(
        private val store: Map<FunctionInfo, FunctionDoc>
) : DocStore {

    override fun get(id: FunctionInfo) = store[id]

}

class SimpleMultiDocStore(
        private val stores: Collection<DocStore>
) : DocStore {

    override fun get(id: FunctionInfo) = stores.asSequence().mapNotNull {
        it[id]
    }.firstOrNull()

}
