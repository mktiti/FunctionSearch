package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.TypeSignature.DirectSignature
import com.mktiti.fsearch.core.fit.TypeSignature.GenericSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.liftNull
import kotlinx.serialization.Serializable

@Serializable
sealed class FunIdParam {

    companion object {
        fun equals(a: FunIdParam, b: FunIdParam, allowSimpleName: Boolean = false): Boolean = if (allowSimpleName) {
            a.equalsAllowSimple(b)
        } else {
            a == b
        }
    }

    @Serializable
    data class Type(
            val info: MinimalInfo
    ) : FunIdParam() {

        override fun equalsAllowSimple(other: FunIdParam)
                = other is Type && info.simpleName == other.info.simpleName &&
                  (info.packageName == other.info.packageName || info.packageName.isEmpty() || other.info.packageName.isEmpty())

    }

    @Serializable
    data class Array(
            val arg: FunIdParam
    ) : FunIdParam() {

        override fun equalsAllowSimple(other: FunIdParam)
                = other is Array && arg.equalsAllowSimple(other.arg)

    }

    @Serializable
    data class TypeParam(
            val sign: String
    ) : FunIdParam() {

        override fun equalsAllowSimple(other: FunIdParam)
                = other is TypeParam && sign == other.sign

    }

    protected abstract fun equalsAllowSimple(other: FunIdParam): Boolean

}

enum class FunInstanceRelation {
    INSTANCE, STATIC, CONSTRUCTOR
}

@Serializable
data class FunctionInfo(
        val file: MinimalInfo,
        val relation: FunInstanceRelation,
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
                instanceRelation: FunInstanceRelation,
                typeParams: List<String>
        ): List<FunIdParam>? {
            fun <T> params(all: List<Pair<String, T>>): List<T> {
                val nonThis = if (instanceRelation != FunInstanceRelation.INSTANCE) all else all.drop(1)
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
                instanceRelation: FunInstanceRelation,
                typeParams: List<String>,
                infoRepo: JavaInfoRepo
        ): FunctionInfo? {
            return paramTypes(infoRepo, signature, instanceRelation, typeParams)?.let {
                FunctionInfo(file, instanceRelation, name, it)
            }
        }
    }

}
