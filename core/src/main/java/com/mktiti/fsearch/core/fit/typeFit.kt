package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.InheritanceLogic.*
import com.mktiti.fsearch.core.fit.SubResult.Continue.ConstraintsKept
import com.mktiti.fsearch.core.fit.SubResult.Continue.Skip
import com.mktiti.fsearch.core.fit.SubResult.Failure
import com.mktiti.fsearch.core.fit.SubResult.TypeArgUpdate
import com.mktiti.fsearch.core.fit.TypeBoundFit.*
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.util.SuperUtil
import com.mktiti.fsearch.core.util.genericString
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.allPermutations
import com.mktiti.fsearch.util.roll
import com.mktiti.fsearch.util.safeCutLast
import java.util.*

class JavaQueryFitter(
        private val infoRepo: JavaInfoRepo,
        private val typeResolver: TypeResolver,
        private val infoFitter: InfoFitter = JavaInfoFitter(infoRepo)
) : QueryFitter, JavaParamFitter {

    companion object {
        private data class FunArgPairing<S, I>(
                val paramArg: S,
                val subArg: I,
                val variance: InheritanceLogic
        )

        private fun <S, I> funPairing(sam: SamType<S>, argIns: List<I>, argOut: I): List<FunArgPairing<S, I>> {
            val inPairs = sam.inputs.zip(argIns) { p, s -> FunArgPairing(p, s, COVARIANCE) }
            val outPair = FunArgPairing(sam.output, argOut, CONTRAVARIANCE)

            return (inPairs + outPair)
        }
    }

    private val boundFitter = JavaTypeBoundFitter(infoRepo, this, typeResolver)

    private fun <I : CompleteMinInfo<*>, T : Type<I>> TypeHolder<I, T>.resolve(): T? = with(typeResolver)

    private fun NonGenericType.anySupers(): Sequence<TypeHolder.Static> = SuperUtil.resolveSupers(infoRepo, typeResolver, this)

    override fun subStatic(
            argPar: TypeHolder.Static,
            subPar: TypeHolder.Static,
            variance: InheritanceLogic
    ): Boolean {
        return if (infoFitter.fit(argPar, subPar)) {
            return argPar.info.args.zipIfSameLength(subPar.info.args)?.all { (argParPar, subParPar) ->
                subStatic(argParPar.holder(), subParPar.holder(), INVARIANCE)
            } ?: false

            /*
            val resolvedArg = argPar.resolve() ?: return false
            val resolvedSub = subPar.resolve() ?: return false

            resolvedArg.typeArgs.zipIfSameLength(resolvedSub.typeArgs)?.all { (argParPar, subParPar) ->
                subStatic(argParPar, subParPar, INVARIANCE)
            } ?: false
             */
        } else {
            val resolvedArg = argPar.resolve() ?: return false
            resolvedArg.samType?.let { paramSam ->
                val funParamCount = infoRepo.ifFunParamCount(subPar.info.base) ?: return@let
                if (funParamCount == paramSam.inputs.size) {
                    val anyMatches = subPar.info.args.dropLast(1).allPermutations().any { subOrderedIns ->
                        funPairing(paramSam, subOrderedIns, subPar.info.args.last()).all { (funParArg, funSubArg, variance) ->
                            subStatic(funParArg, funSubArg.holder(), variance)
                        }
                    }

                    if (anyMatches) {
                        return true
                    }
                }
            }

            when (variance) {
                INVARIANCE -> false
                COVARIANCE -> {
                    subPar.resolve()?.anySupers()?.any { superInfo ->
                        subStatic(argPar, superInfo, variance)
                    } ?: false
                }
                CONTRAVARIANCE -> {
                    resolvedArg.anySupers().any { superInfo ->
                        subStatic(superInfo, subPar, variance)
                    }
                }
            }
        }
    }

    private fun subParamSub(
            ctx: List<TypeParameter>,
            arg: ParamSubstitution,
            type: TypeHolder.Static
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
            argPar: TypeHolder.Dynamic,
            subPar: TypeHolder.Static,
            variance: InheritanceLogic
    ): SubResult {
        fun Boolean.asResult(): SubResult = if (this) ConstraintsKept else Failure

        return if (infoFitter.fit(argPar, subPar)) {
            val zipped = argPar.info.args.zipIfSameLength(subPar.info.args) ?: return Failure

            zipped.map { (argParPar, subParPar) ->
                subAny(argCtx, argParPar, subParPar.holder(), INVARIANCE)
            }.roll<SubResult, SubResult>(ConstraintsKept) { status, res ->
                when (res) {
                    ConstraintsKept -> status to false
                    Skip -> Skip to false
                    else -> res to true
                }
            }

        } else {
            fun List<SubResult>.rollResult(anyEnough: Boolean): SubResult = roll<SubResult, SubResult>(Failure) { _, res ->
                res to when (res) {
                    Failure -> !anyEnough
                    ConstraintsKept -> anyEnough
                    Skip -> anyEnough
                    is TypeArgUpdate -> true
                }
            }

            val resolvedArg = argPar.resolve() ?: return Failure
            resolvedArg.samType?.let { paramSam ->
                val funParamCount = infoRepo.ifFunParamCount(subPar.info.base) ?: return@let
                if (funParamCount == paramSam.inputs.size) {
                    val (subIns, subOut) = subPar.info.args.safeCutLast() ?: return@let
                    val result = funPairing(paramSam, subIns, subOut).map { (funParArg, funSubArg, variance) ->
                        subAny(argCtx, funParArg, funSubArg.holder(), variance)
                    }.rollResult(anyEnough = false)

                    when (result) {
                        ConstraintsKept -> return ConstraintsKept
                        Skip -> return Skip
                        is TypeArgUpdate -> return result
                        Failure -> return@let
                    }
                }
            }

            when (variance) {
                INVARIANCE -> Failure
                COVARIANCE -> {
                    subPar.resolve()?.anySupers()?.asIterable()?.map { superInfo ->
                        subDynamic(argCtx, argPar, superInfo, variance)
                    }?.rollResult(anyEnough = true) ?: Failure
                }
                CONTRAVARIANCE -> {
                    resolvedArg.superTypes.asSequence().asIterable().map { superType ->
                        when (superType) {
                            is TypeHolder.Static -> subStatic(superType, subPar, variance).asResult()
                            is TypeHolder.Dynamic -> subDynamic(argCtx, superType, subPar, variance)
                        }
                    }.rollResult(anyEnough = true)
                }
            }
        }
    }

    override fun subAny(
            context: List<TypeParameter>,
            argPar: ApplicationParameter,
            subType: TypeHolder.Static,
            variance: InheritanceLogic
    ): SubResult {
        return when (argPar) {
            is SelfSubstitution -> Failure
            is ParamSubstitution -> {
                subParamSub(context, argPar, subType)
            }
            is TypeSubstitution<*, *> -> {
                when (val holder = argPar.holder) {
                    is TypeHolder.Static -> {
                        if (subStatic(holder, subType, variance)) {
                            ConstraintsKept
                        } else {
                            Failure
                        }
                    }
                    is TypeHolder.Dynamic -> {
                        subDynamic(context, holder, subType, variance)
                    }
                }
            }
            is BoundedWildcard -> {
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
                    is TypeSubstitution<*, *> -> param.dynamicApply(newApplyArgs) ?: return null
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
                varianceCtx = Collections.nCopies(query.inputParameters.size, COVARIANCE) + CONTRAVARIANCE
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
                when (val subResult = subAny(matchingCtx.funCtx.typeParams, funArg, TypeHolder.Static.Direct(queryArg), variance)) {
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
