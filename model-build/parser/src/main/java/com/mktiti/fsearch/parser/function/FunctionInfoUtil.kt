package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.intermediate.TypeParamInfo.*

object FunctionInfoUtil {

    private fun type(infoRepo: JavaInfoRepo, sub: IntStaticCmi): IntFunIdParam? = if (sub.base == infoRepo.arrayType.toIntMinInfo()) {
        sub.args.getOrNull(0)?.let { arrayArg ->
            type(infoRepo, arrayArg)?.let(IntFunIdParam::Array)
        }
    } else {
        IntFunIdParam.Type(sub.base)
    }

    private fun type(typeParams: List<TemplateTypeParamInfo>, infoRepo: JavaInfoRepo, param: TypeParamInfo): IntFunIdParam? {
        return when (param) {
            is Direct -> type(infoRepo, param.arg.complete())
            is Sat -> type(infoRepo, param.sat)
            is Dat -> {
                val rootParam = param.dat.template
                if (rootParam == infoRepo.arrayType.toIntMinInfo()) {
                    param.dat.args.getOrNull(0)?.let { arrayArg ->
                        type(typeParams, infoRepo, arrayArg)?.let(IntFunIdParam::Array)
                    }
                } else {
                    IntFunIdParam.Type(rootParam)
                }
            }
            is Param -> {
                val sign = typeParams.getOrNull(param.param)?.sign ?: return null
                IntFunIdParam.TypeParam(sign)
            }
            SelfRef, Wildcard, is BoundedWildcard -> null
        }
    }

    private fun mapSignature(
            signature: FunSignatureInfo<*>,
            instanceRelation: IntFunInstanceRelation,
            infoRepo: JavaInfoRepo
    ): List<IntFunIdParam>? {
        fun <T> params(all: List<Pair<String, T>>): List<T> {
            val nonThis = if (instanceRelation != IntFunInstanceRelation.INSTANCE) all else all.drop(1)
            return nonThis.map { it.second }
        }

        return when (signature) {
            is FunSignatureInfo.Direct -> {
                params(signature.inputs).map { param ->
                    type(infoRepo, param)
                }
            }
            is FunSignatureInfo.Generic -> {
                params(signature.inputs).map { param ->
                    type(signature.typeParams, infoRepo, param)
                }
            }
        }.liftNull()
    }

    fun fromSignatureInfo(
            file: IntMinInfo,
            name: String,
            signature: FunSignatureInfo<*>,
            instanceRelation: IntFunInstanceRelation,
            infoRepo: JavaInfoRepo
    ): IntFunInfo? {
        return mapSignature(signature, instanceRelation, infoRepo)?.let {
            IntFunInfo(file, instanceRelation, name, it)
        }
    }

}