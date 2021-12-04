package com.mktiti.fsearch.model.build.intermediate

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.model.build.intermediate.IntFunDoc.Companion.toInt

data class IntFunDocEntry(
        val info: IntFunInfo,
        val doc: IntFunDoc
)

data class FunDocMap(
        val map: List<IntFunDocEntry>
) {

    constructor(map: Map<FunctionInfo, FunctionDoc>) : this(map.map { (i, d) ->
        IntFunDocEntry(i.toInt(), d.toInt())
    })

    companion object {
        fun empty() = FunDocMap(emptyList())
    }

    fun convertMap(): Map<FunctionInfo, FunctionDoc> = map.map { (k, v) ->
        k.convert() to v.convert()
    }.toMap()

}

data class IntFunDoc(
        val link: String? = null,
        val paramNames: List<String>? = null,
        val shortInfo: String? = null,
        val longInfo: String? = null
) {

    companion object {
        internal fun FunctionDoc.toInt() = IntFunDoc(link, paramNames, shortInfo, longInfo)
    }

    fun convert() = FunctionDoc(link, paramNames, shortInfo, longInfo)

}