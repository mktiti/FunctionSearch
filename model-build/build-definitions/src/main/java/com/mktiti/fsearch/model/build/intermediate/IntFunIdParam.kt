package com.mktiti.fsearch.model.build.intermediate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.mktiti.fsearch.core.cache.InfoCache
import com.mktiti.fsearch.core.cache.NopInfoCache
import com.mktiti.fsearch.core.fit.FunIdParam

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
sealed class IntFunIdParam {

    companion object {
        fun equals(a: IntFunIdParam, b: IntFunIdParam, allowSimpleName: Boolean = false): Boolean = if (allowSimpleName) {
            a.equalsAllowSimple(b)
        } else {
            a == b
        }

        internal fun FunIdParam.toInt(): IntFunIdParam = when (this) {
            is FunIdParam.Array -> Array(arg.toInt())
            is FunIdParam.Type -> Type(info.toIntMinInfo())
            is FunIdParam.TypeParam -> TypeParam(sign)
        }
    }

        data class Type(
            val info: IntMinInfo
    ) : IntFunIdParam() {

        override fun equalsAllowSimple(other: IntFunIdParam)
                = other is Type && info.simpleName == other.info.simpleName &&
                (info.packageName == other.info.packageName || info.packageName.isEmpty() || other.info.packageName.isEmpty())

        override fun convert(cache: InfoCache) = FunIdParam.Type(info.toMinimalInfo(cache))

    }

        data class Array(
            val arg: IntFunIdParam
    ) : IntFunIdParam() {

        override fun equalsAllowSimple(other: IntFunIdParam)
                = other is Array && arg.equalsAllowSimple(other.arg)

        override fun convert(cache: InfoCache) = FunIdParam.Array(arg.convert())

    }

        data class TypeParam(
            val sign: String
    ) : IntFunIdParam() {

        override fun equalsAllowSimple(other: IntFunIdParam)
                = other is TypeParam && sign == other.sign

        override fun convert(cache: InfoCache) = FunIdParam.TypeParam(sign)

    }

    protected abstract fun equalsAllowSimple(other: IntFunIdParam): Boolean

    abstract fun convert(cache: InfoCache = NopInfoCache): FunIdParam

}