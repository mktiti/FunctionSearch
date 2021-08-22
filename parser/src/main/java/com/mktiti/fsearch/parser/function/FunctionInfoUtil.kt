package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.service.indirect.FunSignatureInfo
import com.mktiti.fsearch.parser.service.indirect.TemplateTypeParamInfo
import com.mktiti.fsearch.parser.service.indirect.TypeParamInfo
import com.mktiti.fsearch.parser.service.indirect.TypeParamInfo.*

object FunctionInfoUtil {

    private fun type(infoRepo: JavaInfoRepo, sub: CompleteMinInfo.Static): FunIdParam? = if (sub.base == infoRepo.arrayType) {
        sub.args.getOrNull(0)?.let { arrayArg ->
            type(infoRepo, arrayArg)?.let(FunIdParam::Array)
        }
    } else {
        FunIdParam.Type(sub.base)
    }

    private fun type(typeParams: List<TemplateTypeParamInfo>, infoRepo: JavaInfoRepo, param: TypeParamInfo): FunIdParam? {
        return when (param) {
            is Direct -> type(infoRepo, param.arg.complete())
            is Sat -> type(infoRepo, param.sat)
            is Dat -> {
                val rootParam = param.dat.template
                if (rootParam == infoRepo.arrayType) {
                    param.dat.args.getOrNull(0)?.let { arrayArg ->
                        type(typeParams, infoRepo, arrayArg)?.let(FunIdParam::Array)
                    }
                } else {
                    FunIdParam.Type(rootParam)
                }
            }
            is Param -> {
                val sign = typeParams.getOrNull(param.param)?.sign ?: return null
                FunIdParam.TypeParam(sign)
            }
            SelfRef, Wildcard, is BoundedWildcard -> null
        }
    }

    private fun mapSignature(
            signature: FunSignatureInfo<*>,
            instanceRelation: FunInstanceRelation,
            infoRepo: JavaInfoRepo
    ): List<FunIdParam>? {
        fun <T> params(all: List<Pair<String, T>>): List<T> {
            val nonThis = if (instanceRelation != FunInstanceRelation.INSTANCE) all else all.drop(1)
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
            file: MinimalInfo,
            name: String,
            signature: FunSignatureInfo<*>,
            instanceRelation: FunInstanceRelation,
            infoRepo: JavaInfoRepo
    ): FunctionInfo? {
        return mapSignature(signature, instanceRelation, infoRepo)?.let {
            FunctionInfo(file, instanceRelation, name, it)
        }
    }

}