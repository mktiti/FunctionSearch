package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.InheritanceLogic.*
import com.mktiti.fsearch.core.fit.SubResult.Continue.ConstraintsKept
import com.mktiti.fsearch.core.fit.SubResult.Continue.Skip
import com.mktiti.fsearch.core.fit.SubResult.Failure
import com.mktiti.fsearch.core.fit.SubResult.TypeArgUpdate
import com.mktiti.fsearch.core.fit.TypeBoundFit.*
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.Companion.wrap
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.util.genericString
import com.mktiti.fsearch.core.util.nList
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.allPermutations
import com.mktiti.fsearch.util.roll

class JavaQueryFitter(
        private val typeResolver: TypeResolver
) : QueryFitter, JavaParamFitter {

    private val boundFitter = JavaTypeBoundFitter(this, typeResolver)

    private fun CompleteMinInfo.Static.resolve(): NonGenericType? = typeResolver[this]
    private fun CompleteMinInfo.Dynamic.resolve(): DynamicAppliedType? = typeResolver[this]

    override fun subStatic(
            argPar: CompleteMinInfo.Static,
            subPar: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): Boolean {
        return if (argPar == subPar) {
            val resolvedArg = argPar.resolve() ?: return false
            val resolvedSub = subPar.resolve() ?: return false

            resolvedArg.typeArgs.zipIfSameLength(resolvedSub.typeArgs)?.all { (argParPar, subParPar) ->
                subStatic(argParPar, subParPar, INVARIANCE)
            } ?: false
        } else {
            when (variance) {
                INVARIANCE -> false
                COVARIANCE -> {
                    subPar.resolve()?.superTypes?.asSequence()?.any { superInfo ->
                        subStatic(argPar, superInfo, variance)
                    } ?: false
                }
                CONTRAVARIANCE -> {
                    argPar.resolve()?.superTypes?.asSequence()?.any { superInfo ->
                        subStatic(superInfo, subPar, variance)
                    } ?: false
                }
            }
        }
    }

    private fun subParamSub(
            ctx: List<TypeParameter>,
            arg: ParamSubstitution,
            type: CompleteMinInfo.Static
    ): SubResult {
        val referenced = ctx.getOrNull(arg.param) ?: return Failure
        return when (val fitRes = boundFitter.fits(referenced.bounds, type)) {
            Fit -> TypeArgUpdate(arg.param, type)
            YetUncertain -> Skip
            is Requires -> fitRes.update
            Unfit -> Failure
        }
    }

    override fun subDynamic(
            argCtx: List<TypeParameter>,
            argPar: CompleteMinInfo.Dynamic,
            subPar: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): SubResult {
        fun Boolean.asResult(): SubResult = if (this) ConstraintsKept else Failure

        return if (argPar.base == subPar.base) {
            val resolvedArg = argPar.resolve() ?: return Failure
            val resolvedSub = subPar.resolve() ?: return Failure
            val zipped = resolvedArg.typeArgMapping.zipIfSameLength(resolvedSub.typeArgs) ?: return Failure

            zipped.map { (argParPar, subParPar) ->
                subAny(argCtx, argParPar, subParPar, INVARIANCE)
            }.roll<SubResult, SubResult>(ConstraintsKept) { status, res ->
                when (res) {
                    ConstraintsKept -> status to false
                    Skip -> Skip to false
                    else -> res to true
                }
            }
        } else {
            fun List<SubResult>.rollResult(): SubResult = roll<SubResult, SubResult>(Failure) { _, res ->
                res to when (res) {
                    Failure -> false
                    ConstraintsKept -> true
                    Skip -> true
                    is TypeArgUpdate -> true
                }
            }

            when (variance) {
                INVARIANCE -> Failure
                COVARIANCE -> {
                    val resolvedSub = subPar.resolve() ?: return Failure
                    resolvedSub.superTypes.asSequence().asIterable().map { superInfo ->
                        subDynamic(argCtx, argPar, superInfo, variance)
                    }.rollResult()
                }
                CONTRAVARIANCE -> {
                    val resolvedArg = argPar.resolve() ?: return Failure
                    resolvedArg.superTypes.asSequence().asIterable().map { superType ->
                        when (superType) {
                            is CompleteMinInfo.Static -> subStatic(superType, subPar, variance).asResult()
                            is CompleteMinInfo.Dynamic -> subDynamic(argCtx, superType, subPar, variance)
                        }
                    }.rollResult()
                }
            }
        }
    }

    override fun subAny(
            context: List<TypeParameter>,
            argPar: ApplicationParameter,
            subType: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): SubResult {
        return when (argPar) {
            is SelfSubstitution -> Failure
            is ParamSubstitution -> {
                subParamSub(context, argPar, subType)
            }
            is DynamicTypeSubstitution -> {
                subDynamic(context, argPar.type, subType, variance)
            }
            is StaticTypeSubstitution -> {
                if (subStatic(argPar.type, subType, variance)) {
                    ConstraintsKept
                } else {
                    Failure
                }
            }
            is Wildcard.Direct -> ConstraintsKept
            is Wildcard.Bounded -> {
                when (subAny(context, argPar.param, subType, argPar.subVariance)) {
                    Failure -> Failure
                    ConstraintsKept -> ConstraintsKept
                    Skip -> Skip
                    is TypeArgUpdate -> Skip
                }
            }
        }
    }

    private sealed class FullResult {
        object Failure : FullResult()

        object Success : FullResult()

        data class Update(
                val fits: Set<Int>,
                val update: TypeArgUpdate
        ) : FullResult()
    }

    private tailrec fun transformContext(startContext: MatchingContext, mapping: FittingMap, code: (context: MatchingContext) -> FullResult): FittingMap? {
        return when (val result = code(startContext)) {
            is FullResult.Failure -> null
            is FullResult.Success -> mapping
            is FullResult.Update -> {
                val update = result.update
                val updatedMapping = mapping + (startContext.funCtx.typeParams[update.arg] to (update.type.resolve() ?: return null))
                val updatedContext = startContext.transform(update, result.fits) ?: return null
                transformContext(updatedContext, updatedMapping, code)
            }
        }
    }

    private data class SignatureContext(
            val typeParams: List<TypeParameter>,
            val parameters: List<Substitution>
    ) {

        companion object {
            fun fromTypeSignature(typeSignature: TypeSignature): SignatureContext {
                return SignatureContext(
                        typeParams = typeSignature.typeParameters,
                        parameters = typeSignature.inputParameters.map { (_, param) -> param } + typeSignature.output
                )
            }
        }

        fun apply(update: TypeArgUpdate): SignatureContext? {
            val unchangedTypeParams = typeParams.take(update.arg)
            val prefixRefs = (0 until update.arg).map(Substitution::ParamSubstitution)

            val init: Pair<List<TypeParameter>, List<Substitution>> = unchangedTypeParams to (prefixRefs + StaticTypeSubstitution(update.type))

            val (newTps, newApplyArgs) = typeParams.drop(update.arg + 1).fold(init) { (acc, applyArgs), typeParam ->
                val applied = typeParam.apply(applyArgs) ?: return null
                (acc + applied) to (applyArgs + ParamSubstitution(acc.size))
            }
            val updatedParams = parameters.map { param ->
                when (param) {
                    is SelfSubstitution -> return null
                    is ParamSubstitution -> {
                        when {
                            param.param < update.arg -> param
                            param.param == update.arg -> StaticTypeSubstitution(update.type)
                            else -> ParamSubstitution(param.param - 1)
                        }
                    }
                    is DynamicTypeSubstitution -> wrap(param.type.dynamicApply(newApplyArgs) ?: return null)
                    is StaticTypeSubstitution -> param
                }
            }

            return SignatureContext(newTps, updatedParams)
        }
    }

    private data class ParamPair(
            val index: Int,
            val funParam: ApplicationParameter,
            val queryParam: NonGenericType,
            val variance: InheritanceLogic
    )

    private data class MatchingContext(
            val funCtx: SignatureContext,
            val query: QueryType,
            val varianceCtx: List<InheritanceLogic>,
            val skipPairings: Set<Int> = emptySet()
    ) {

        val pairings: Iterable<ParamPair> =
                funCtx.parameters.zipIfSameLength(query.allParams)
                        ?.zipIfSameLength(varianceCtx)
                        ?.mapIndexed { i, (params, variance) ->
                            ParamPair(
                                    index = i,
                                    funParam = params.first,
                                    queryParam = params.second,
                                    variance = variance
                            )
                        }?.filter { it.index !in skipPairings }!!

        constructor(query: QueryType, function: FunctionObj) : this(
                funCtx = SignatureContext.fromTypeSignature(function.signature),
                query = query,
                varianceCtx = nList(COVARIANCE, query.inputParameters.size) + CONTRAVARIANCE
        )

        fun transform(update: TypeArgUpdate, skippable: Set<Int>): MatchingContext? {
            return copy(
                    funCtx = funCtx.apply(update) ?: return null,
                    skipPairings = skipPairings + skippable
            )
        }

        override fun toString(): String {
            return "Type signature match: ${if (funCtx.typeParams.isNotEmpty()) "" else funCtx.typeParams.genericString()} (${funCtx.parameters.dropLast(1).joinToString()}) -> ${funCtx.parameters.last()}"
        }

    }

    // TODO - rework performance
    override fun fitsOrderedQuery(query: QueryType, function: FunctionObj): FittingMap? {
        data class MatchRoll(
                val status: SubResult = ConstraintsKept,
                val fitIndices: Set<Int> = emptySet()
        ) {

            fun skipped() = copy(status = Skip) to false

            fun withFit(index: Int) = copy(fitIndices = fitIndices + index) to false

            fun terminal(status: SubResult) = copy(status = status) to true
        }

        return transformContext(MatchingContext(query, function), FittingMap(query, function.signature)) { matchingCtx ->
            val essentialResult = matchingCtx.pairings.toList().roll(MatchRoll()) { statAcc, (index, funArg, queryArg, variance) ->
                when (val subResult = subAny(matchingCtx.funCtx.typeParams, funArg, queryArg.completeInfo, variance)) {
                    ConstraintsKept -> statAcc.withFit(index)
                    Skip -> statAcc.skipped()
                    else -> statAcc.terminal(subResult)
                }
            }

            when (val stat = essentialResult.status) {
                Failure -> FullResult.Failure
                is TypeArgUpdate -> FullResult.Update(essentialResult.fitIndices, stat)
                is ConstraintsKept -> FullResult.Success
                is Skip -> FullResult.Failure
            }
        }
    }

    override fun fitsQuery(query: QueryType, function: FunctionObj): FittingMap? {
        if (query.inputParameters.size != function.signature.inputParameters.size) {
            return null
        }

        return query.inputParameters.allPermutations().asSequence().mapNotNull { inputsOrdered ->
            fitsOrderedQuery(query.copy(inputParameters = inputsOrdered), function)
        }.firstOrNull()
    }


}
