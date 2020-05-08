import ApplicationParameter.Substitution
import ApplicationParameter.Substitution.ParamSubstitution
import ApplicationParameter.Substitution.SelfSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.Companion.wrap
import ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import ApplicationParameter.Wildcard
import InheritanceLogic.*
import SubResult.Continue.ConstraintsKept
import SubResult.Continue.Skip
import SubResult.Failure
import SubResult.TypeArgUpdate
import Type.DynamicAppliedType
import TypeBoundFit.*

enum class InheritanceLogic {
    INVARIANCE, COVARIANCE, CONTRAVARIANCE
}

typealias FunContext = List<TypeParameter>

sealed class SubResult {
    object Failure : SubResult()

    sealed class Continue : SubResult() {
        object ConstraintsKept : Continue()

        object Skip : Continue()
    }

    data class TypeArgUpdate(val arg: Int, val type: Type.NonGenericType) : SubResult()
}

fun subStatic(
    argPar: Type.NonGenericType,
    subPar: Type.NonGenericType,
    variance: InheritanceLogic
): Boolean {
    return if (argPar.info == subPar.info) {
        argPar.typeArgs.zipIfSameLength(subPar.typeArgs)?.all { (argParPar, subParPar) ->
            subStatic(argParPar, subParPar, INVARIANCE)
        } ?: false
    } else {
        when (variance) {
            INVARIANCE -> false
            COVARIANCE -> {
                subPar.superTypes.asSequence().any { superType ->
                    subStatic(argPar, superType.type, variance)
                }
            }
            CONTRAVARIANCE -> {
                argPar.superTypes.asSequence().any { superType ->
                    subStatic(superType.type, subPar, variance)
                }
            }
        }
    }
}

fun subParamSub(
    ctx: List<TypeParameter>,
    arg: ParamSubstitution,
    type: Type.NonGenericType
): SubResult {
    val referenced = ctx.getOrNull(arg.param) ?: return Failure
    return when (val fitRes = referenced.fits(type)) {
        Fit -> TypeArgUpdate(arg.param, type)
        YetUncertain -> Skip
        is Requires -> fitRes.update
        Unfit -> Failure
    }
}

fun subDynamic(
    argCtx: List<TypeParameter>,
    argPar: DynamicAppliedType,
    subPar: Type.NonGenericType,
    variance: InheritanceLogic
): SubResult {
    fun Boolean.asResult(): SubResult = if (this) ConstraintsKept else Failure

    return if (argPar.info == subPar.info) {
        val zipped = argPar.typeArgMapping.zipIfSameLength(subPar.typeArgs) ?: return Failure

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
                subPar.superTypes.asSequence().asIterable().map { superType ->
                    subDynamic(argCtx, argPar, superType.type, variance)
                }.rollResult()
            }
            CONTRAVARIANCE -> {
                argPar.superTypes.asSequence().asIterable().map { superType ->
                    when (val stt = superType.type) {
                        is Type.NonGenericType -> subStatic(stt, subPar, variance).asResult()
                        is DynamicAppliedType -> subDynamic(argCtx, stt, subPar, variance)
                    }
                }.rollResult()
            }
        }
    }
}

fun subAny(
    context: FunContext,
    argPar: ApplicationParameter,
    subType: Type.NonGenericType,
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
        is Wildcard.BoundedWildcard -> {
            when (subAny(context, argPar.param, subType, argPar.subVariance)) {
                Failure -> Failure
                ConstraintsKept -> ConstraintsKept
                Skip -> Skip
                is TypeArgUpdate -> Skip
            }
        }
    }
}

data class FittingMap(
    val orderedQuery: QueryType,
    val funSignature: TypeSignature,
    val typeParamMapping: List<Pair<TypeParameter, Type.NonGenericType>> = emptyList()
) {

    operator fun plus(typeParam: Pair<TypeParameter, Type.NonGenericType>): FittingMap = copy(
        typeParamMapping = typeParamMapping + typeParam
    )

    override fun toString() = buildString {
        append("Query fit, where ")
        append(typeParamMapping.genericString { (param, type) -> "${param.sign} = $type" })
        val inputPairs = funSignature.inputParameters.zip(orderedQuery.inputParameters)
        val paramMap = inputPairs.joinToString(prefix = " (", separator = ", ", postfix = ") -> ") { (funIn, queryIn) ->
            "${funIn.first}: $queryIn"
        }
        append(paramMap)
        append(orderedQuery.output)
    }

}

sealed class FullResult {
    object Failure : FullResult()

    object Success : FullResult()

    data class Update(
        val fits: Set<Int>,
        val update: TypeArgUpdate
    ) : FullResult()
}

tailrec fun transformContext(startContext: MatchingContext, mapping: FittingMap, code: (context: MatchingContext) -> FullResult): FittingMap? {
    return when (val result = code(startContext)) {
        is FullResult.Failure -> null
        is FullResult.Success -> mapping
        is FullResult.Update -> {
            val update = result.update
            val updatedMapping = mapping + (startContext.funCtx.typeParams[update.arg] to update.type)
            val updatedContext = startContext.transform(update, result.fits) ?: return null
            transformContext(updatedContext, updatedMapping, code)
        }
    }
}

data class SignatureContext(
    val typeParams: FunContext,
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
                is DynamicTypeSubstitution -> wrap(param.type.forceApply(newApplyArgs))
                is StaticTypeSubstitution -> param
            }
        }

        return SignatureContext(newTps, updatedParams)
    }
}

data class ParamPair(
    val index: Int,
    val funParam: ApplicationParameter,
    val queryParam: Type.NonGenericType,
    val variance: InheritanceLogic
)

data class MatchingContext(
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
fun fitsOrderedQuery(query: QueryType, function: FunctionObj): FittingMap? {
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
            when (val subResult = subAny(matchingCtx.funCtx.typeParams, funArg, queryArg, variance)) {
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

fun fitsQuery(query: QueryType, function: FunctionObj): FittingMap? {
    if (query.inputParameters.size != function.signature.inputParameters.size) {
        return null
    }

    return query.inputParameters.allPermutations().asSequence().mapNotNull { inputsOrdered ->
        fitsOrderedQuery(query.copy(inputParameters = inputsOrdered), function)
    }.firstOrNull()
}
