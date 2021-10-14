package com.mktiti.fsearch.model.build.intermediate

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.model.build.intermediate.IntFunDoc.Companion.toInt
import kotlinx.serialization.Serializable

@Serializable
data class FunDocMap(
        val map: List<Pair<IntFunInfo, IntFunDoc>>
) {

    constructor(map: Map<FunctionInfo, FunctionDoc>) : this(map.map { (i, d) ->
        i.toInt() to d.toInt()
    })

    companion object {
        fun empty() = FunDocMap(emptyList())
    }

    fun convertMap(): Map<FunctionInfo, FunctionDoc> = map.map { (k, v) ->
        k.convert() to v.convert()
    }.toMap()

}

@Serializable
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