package com.mktiti.fsearch.model.build.intermediate

import com.mktiti.fsearch.core.cache.CentralInfoCache
import com.mktiti.fsearch.core.cache.InfoCache
import com.mktiti.fsearch.core.cache.NopInfoCache
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.model.build.intermediate.IntFunIdParam.Companion.toInt
import com.mktiti.fsearch.model.build.intermediate.IntFunInstanceRelation.Companion.toInt

fun MinimalInfo.toIntMinInfo() = IntMinInfo(packageName, simpleName, virtual)

data class IntMinInfo(
        val packageName: List<String>,
        val simpleName: String,
        val virtual: Boolean = false
) {

    fun toMinimalInfo(cache: InfoCache = NopInfoCache) = cache.minimalInfo(packageName, simpleName, virtual)

    fun nameParts(): List<String> = simpleName.split('.')

    fun complete() = IntStaticCmi(this, emptyList())

}

data class IntStaticCmi(
        val base : IntMinInfo,
        val args: List<IntStaticCmi>
) {

    fun convert(cache: InfoCache = NopInfoCache): CompleteMinInfo.Static = CompleteMinInfo.Static(
            base.toMinimalInfo(cache), args.map { it.convert(cache) }
    )

}

enum class IntFunInstanceRelation {
    INSTANCE, STATIC, CONSTRUCTOR;

    companion object {
        internal fun FunInstanceRelation.toInt() = when (this) {
            FunInstanceRelation.INSTANCE -> INSTANCE
            FunInstanceRelation.STATIC -> STATIC
            FunInstanceRelation.CONSTRUCTOR -> CONSTRUCTOR
        }
    }

    fun convert(): FunInstanceRelation = when (this) {
        INSTANCE -> FunInstanceRelation.INSTANCE
        STATIC -> FunInstanceRelation.STATIC
        CONSTRUCTOR -> FunInstanceRelation.CONSTRUCTOR
    }

}

internal fun FunctionInfo.toInt() = IntFunInfo(
        file.toIntMinInfo(),
        relation.toInt(),
        name,
        paramTypes.map { it.toInt() }
)

data class IntFunInfo(
        val file: IntMinInfo,
        val relation: IntFunInstanceRelation,
        val name: String,
        val paramTypes: List<IntFunIdParam>
) {

    fun convert(cache: InfoCache = CentralInfoCache)
        = FunctionInfo(file.toMinimalInfo(cache), relation.convert(), name, paramTypes.map { it.convert() })

}

