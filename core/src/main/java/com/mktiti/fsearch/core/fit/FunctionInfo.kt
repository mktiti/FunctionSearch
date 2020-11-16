package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.TypeSignature.DirectSignature
import com.mktiti.fsearch.core.fit.TypeSignature.GenericSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.liftNull

sealed class FunIdParam {

    data class Type(
            val info: MinimalInfo
    ) : FunIdParam()

    data class Array(
            val arg: FunIdParam
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
        private fun type(typeParams: List<String>, infoRepo: JavaInfoRepo, sub: TypeSubstitution<*, *>): FunIdParam? = with(sub.holder) {
            if (info.base == infoRepo.arrayType) {
                val arrayArg: FunIdParam = when (this) {
                    is TypeHolder.Dynamic -> {
                        // TODO Wildcards?
                        val arg = info.args.singleOrNull() as? ApplicationParameter.Substitution ?: return@with null
                        param(typeParams, infoRepo, arg)
                    }
                    is TypeHolder.Static -> {
                        val arg = TypeSubstitution(info.args.singleOrNull()?.holder() ?: return@with null)
                        type(typeParams, infoRepo, arg)
                    }
                } ?: return@with null

                FunIdParam.Array(arrayArg)
            } else {
                FunIdParam.Type(info.base)
            }
        }

        private fun param(typeParams: List<String>, infoRepo: JavaInfoRepo, sub: ApplicationParameter.Substitution): FunIdParam? {
            return when (sub) {
                is ParamSubstitution -> {
                    val sign = typeParams.getOrNull(sub.param) ?: return null
                    FunIdParam.TypeParam(sign)
                }
                SelfSubstitution -> null
                is TypeSubstitution<*, *> -> type(typeParams, infoRepo, sub)
            }
        }

        fun paramTypes(
                infoRepo: JavaInfoRepo,
                signature: TypeSignature,
                isStatic: Boolean,
                typeParams: List<String>
        ): List<FunIdParam>? {
            fun <T> params(all: List<Pair<String, T>>): List<T> {
                val nonThis = if (isStatic) all else all.drop(1)
                return nonThis.map { it.second }
            }

            return when (signature) {
                is DirectSignature -> {
                    params(signature.inputParameters).map { param ->
                        type(typeParams, infoRepo, param)
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
                            is TypeSubstitution<*, *> -> type(typeParams, infoRepo, param)
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
                typeParams: List<String>,
                infoRepo: JavaInfoRepo
        ): FunctionInfo? {
            return paramTypes(infoRepo, signature, isStatic, typeParams)?.let {
                FunctionInfo(file, isStatic, name, it)
            }
        }
    }

}
