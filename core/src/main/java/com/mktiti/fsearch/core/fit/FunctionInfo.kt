package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.TypeSignature.DirectSignature
import com.mktiti.fsearch.core.fit.TypeSignature.GenericSignature
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.liftNull

sealed class FunIdParam {

    data class Type(
            val info: MinimalInfo
    ) : FunIdParam()

    data class TypeParam(
            val sign: String
    ) : FunIdParam()

}

data class FunctionInfo(
        val file: MinimalInfo,
        val isStatic: Boolean,
        val name: String,
        val paramTypes: List<FunIdParam>
) {

    companion object {
        fun paramTypes(signature: TypeSignature, isStatic: Boolean, typeParams: List<String>): List<FunIdParam>? {
            fun <T> params(all: List<Pair<String, T>>): List<T> {
                val nonThis = if (isStatic) all else all.drop(1)
                return nonThis.map { it.second }
            }

            return when (signature) {
                is DirectSignature -> {
                    params(signature.inputParameters).map { param ->
                        FunIdParam.Type(param.holder.info.base)
                    }
                }
                is GenericSignature -> {
                    params(signature.inputParameters).map { param ->
                        when (param) {
                            is ParamSubstitution -> {
                                val sign = typeParams.getOrNull(param.param) ?: return@map null
                                FunIdParam.TypeParam(sign)
                            }
                            SelfSubstitution -> null
                            is TypeSubstitution<*, *> -> FunIdParam.Type(param.holder.info.base)
                        }
                    }
                }
            }.liftNull()
        }

        fun fromSignature(
                file: MinimalInfo,
                name: String,
                signature: TypeSignature,
                isStatic: Boolean,
                typeParams: List<String>
        ): FunctionInfo? {
            return paramTypes(signature, isStatic, typeParams)?.let {
                FunctionInfo(file, isStatic, name, it)
            }
        }
    }

}
