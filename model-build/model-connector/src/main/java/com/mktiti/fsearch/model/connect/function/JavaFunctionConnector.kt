package com.mktiti.fsearch.model.connect.function

import arrow.core.Either
import com.mktiti.fsearch.core.cache.InfoCache
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.Dynamic
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.util.InfoMap
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.service.FunctionCollection
import com.mktiti.fsearch.model.build.service.FunctionConnector
import com.mktiti.fsearch.util.logWarning
import com.mktiti.fsearch.util.logger
import com.mktiti.fsearch.util.splitMap

class JavaFunctionConnector(
        private val internCache: InfoCache
) : FunctionConnector {

    private val log = logger()

    private fun <P, O> List<Pair<String, P>>.convertIns(converter: (P) -> O): List<Pair<String, O>> = map { (name, param) ->
        name to converter(param)
    }

    private fun <P, O> List<Pair<String, P>>.convertInsOpt(converter: (P) -> O?): List<Pair<String, O>>? = map { (name, param) ->
        converter(param)?.let {
            name to it
        }
    }.liftNull()

    private fun CompleteMinInfo.Static.convertToSts(): StaticTypeSubstitution {
        val holder = if (args.isEmpty()) {
            base.complete()
        } else {
            this
        }.holder()

        return StaticTypeSubstitution(holder)
    }

    private fun FunSignatureInfo.Direct.connectDirect() = TypeSignature.DirectSignature(
            inputParameters = inputs.convertIns { it.convert().convertToSts() },
            output = output.convert().convertToSts()
    )

    private fun TemplateTypeParamInfo.convert(): TypeParameter? {
        val convertedBounds = bounds.map { it.convertSub() }.liftNull() ?: return null
        return TypeParameter(
                sign = sign,
                bounds = TypeBounds(convertedBounds.toSet())
        )
    }

    private fun TypeParamInfo.convertSub(): Substitution? {
        return convert() as? Substitution
    }

    private fun TypeParamInfo.convert(): ApplicationParameter? {
        return when (this) {
            TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
            TypeParamInfo.SelfRef -> SelfSubstitution
            is TypeParamInfo.BoundedWildcard -> {
                val bound = bound.convertSub() ?: return null
                val dir = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) BoundDirection.UPPER else BoundDirection.LOWER
                Dynamic(param = bound, direction = dir)
            }
            is TypeParamInfo.Direct -> StaticTypeSubstitution(arg.toMinimalInfo().complete().holder())
            is TypeParamInfo.Sat -> StaticTypeSubstitution(sat.convert().holder())
            is TypeParamInfo.Dat -> {
                val convertedArgs = dat.args.map { it.convert() }.liftNull() ?: return null
                val convertedDat = TypeHolder.Dynamic.Indirect(
                        CompleteMinInfo.Dynamic(dat.template.toMinimalInfo(), convertedArgs)
                )
                TypeSubstitution(convertedDat)
            }
            is TypeParamInfo.Param -> Substitution.ParamSubstitution(param)
        }
    }

    private fun FunSignatureInfo.Generic.connectGeneric(): TypeSignature.GenericSignature? {
        val converterTps = typeParams.map { it.convert() }.liftNull() ?: return null
        val convertedIns = inputs.convertInsOpt { it.convertSub() } ?: return null
        val convertedOut = output.convertSub() ?: return null

        return TypeSignature.GenericSignature(
                typeParameters = converterTps,
                inputParameters = convertedIns,
                output = convertedOut
        )
    }

    private fun connectSignature(signature: FunSignatureInfo<*>): TypeSignature? = when (signature) {
        is FunSignatureInfo.Direct -> signature.connectDirect()
        is FunSignatureInfo.Generic -> signature.connectGeneric()
    }

    private fun connectFunction(info: RawFunInfo): FunctionObj? {
        val converterSignature = connectSignature(info.signature) ?: return null
        return FunctionObj(internCache.funInfo(info.info.convert(internCache)), converterSignature)
    }

    override fun connect(funInfo: FunctionInfoResult): FunctionCollection {
        fun Collection<RawFunInfo>.convertAll(): Pair<List<FunctionObj>, List<FunctionInfo>> {
            return splitMap {
                when (val result = connectFunction(it)) {
                    null -> Either.Right(it.info.convert(internCache))
                    else -> Either.Left(result)
                }
            }
        }

        log.trace("Started connecting function info")

        val (convertedStatics, staticFails) = funInfo.staticFunctions.convertAll()
        staticFails.forEach {
            log.logWarning { "Failed to convert static fun $it" }
        }

        val convertedInstances = funInfo.instanceMethods.map { (key, values) ->
            val (converted, failed) = values.convertAll()
            failed.forEach {
                log.logWarning { "Failed to convert instance fun $it" }
            }
            key.toMinimalInfo(internCache) to converted
        }.toMap()

        return FunctionCollection(convertedStatics, InfoMap.fromMap(convertedInstances))
    }

}