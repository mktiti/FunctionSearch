import ApplicationParameter.ParamSubstitution
import ApplicationParameter.TypeSubstitution.Companion.wrap
import ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import InheritanceLogic.*
import SubResult.*
import SubResult.Continue.ConstraintsKept
import SubResult.Continue.Skip
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
        argPar.typeArgs.zipIfSameLength(subPar.typeArgs)?.all { (argPar, subPar) ->
            subStatic(argPar, subPar, INVARIANCE)
        } ?: false
    } else {
        when (variance) {
            INVARIANCE -> false
            COVARIANCE -> {
                subPar.superTypes.asSequence().mapNotNull { superType ->
                    subStatic(argPar, superType.type, variance)
                }.any()
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argPar.superTypes.asSequence().map { superType ->
                    subStatic(superType.type, subPar, variance)
                }.any()
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
    return when (referenced.fits(ctx, type)) {
        FIT -> TypeArgUpdate(arg.param, type)
        YET_UNCERTAIN -> Skip
        UNFIT -> Failure
    }
}

fun subDynamic(
    argCtx: List<TypeParameter>,
    argPar: DynamicAppliedType,
    subPar: Type.NonGenericType,
    variance: InheritanceLogic
): SubResult {
    fun Boolean.asResult(): SubResult = if (this) ConstraintsKept else Failure

    fun Sequence<SubResult>.realResult(default: SubResult): SubResult = lazyReduce(MatchRoll(status = default)) { status, res ->
        when (res) {
            ConstraintsKept -> status to false
            Skip -> status.copy(skippedAny = true) to false
            else -> MatchRoll(status = res) to true
        }
    }.status

    return if (argPar.info == subPar.info) {
        val zipped = argPar.typeArgMapping.zipIfSameLength(subPar.typeArgs) ?: return Failure

        zipped.asSequence().map { (argPar, subPar) ->
            when (argPar) {
                is ParamSubstitution -> {
                    subParamSub(argCtx, argPar, subPar)
                }
                is DynamicTypeSubstitution -> {
                    subDynamic(argCtx, argPar.type, subPar, INVARIANCE)
                }
                is StaticTypeSubstitution -> {
                    subStatic(argPar.type, subPar, INVARIANCE).asResult()
                }
            }
        }.realResult(ConstraintsKept)
    } else {
        when (variance) {
            INVARIANCE -> Failure
            COVARIANCE -> {
                subPar.superTypes.asSequence().map { superType ->
                    subDynamic(argCtx, argPar, superType.type, variance)
                }.realResult(Failure)
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argPar.superTypes.asSequence().map { superType ->
                    when (val stt = superType.type) {
                        is Type.NonGenericType -> subStatic(stt, subPar, variance).asResult()
                        is DynamicAppliedType -> subDynamic(argCtx, stt, subPar, variance)
                    }
                }.realResult(Failure)
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
    }
}

sealed class FullResult {
    object Failure : FullResult()

    object Success : FullResult()

    data class Update(val update: TypeArgUpdate) : FullResult()
}

tailrec fun transformContext(initial: MatchingContext, code: (context: MatchingContext) -> FullResult): MatchingContext? {
    return when (val midResult = code(initial)) {
        is FullResult.Failure -> null
        is FullResult.Success -> initial
        is FullResult.Update -> {
            val appliedType = initial.transform(midResult.update) ?: return null
            transformContext(appliedType, code)
        }
    }
}

data class SignatureContext(
    val typeParams: FunContext,
    val parameters: List<ApplicationParameter>
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
        val prefixRefs = (0 until update.arg).map(ApplicationParameter::ParamSubstitution)

        val init: Pair<List<TypeParameter>, List<ApplicationParameter>> = unchangedTypeParams to (prefixRefs + StaticTypeSubstitution(update.type))

        val (newTps, newApplyArgs) = typeParams.drop(update.arg + 1).fold(init) { (acc, applyArgs), typeParam ->
            val applied = typeParam.apply(applyArgs) ?: return null
            (acc + applied) to (applyArgs + ParamSubstitution(acc.size))
        }
        val updatedParams = parameters.map { param ->
            when (param) {
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
    val funParam: ApplicationParameter,
    val queryParam: Type.NonGenericType,
    val variance: InheritanceLogic
)

data class MatchingContext(
    val funCtx: SignatureContext,
    val query: QueryType,
    val varianceCtx: List<InheritanceLogic>
) {

    val pairings: Sequence<ParamPair>
        get() = funCtx.parameters.zipIfSameLength(query.allParams)?.zipIfSameLength(varianceCtx)?.map { (params, variance) ->
            ParamPair(
                funParam = params.first,
                queryParam = params.second,
                variance = variance
            )
        }?.asSequence()!!

    constructor(query: QueryType, function: FunctionObj) : this(
        funCtx = SignatureContext.fromTypeSignature(function.signature),
        query = query,
        varianceCtx = nList(COVARIANCE, query.inputParameters.size) + CONTRAVARIANCE
    )

    fun transform(update: TypeArgUpdate): MatchingContext? {
        return copy(funCtx = funCtx.apply(update) ?: return null)
    }

    override fun toString(): String {
        return "Type signature match: ${if (funCtx.typeParams.isNotEmpty()) "" else funCtx.typeParams.genericString()} (${funCtx.parameters.dropLast(1).joinToString()}) -> ${funCtx.parameters.last()}"
    }

}

data class MatchRoll(
    val skippedAny: Boolean = false,
    val status: SubResult = ConstraintsKept
)

// TODO - rework performance
fun fitsOrderedQuery(query: QueryType, function: FunctionObj): MatchingContext? {
    return transformContext(MatchingContext(query, function)) { matchingCtx ->
        val update = matchingCtx.pairings.map { (funArg, queryArg, variance) ->
            subAny(matchingCtx.funCtx.typeParams, funArg, queryArg, variance)
        }.lazyReduce(MatchRoll()) { status, subRes ->
            when (subRes) {
                ConstraintsKept -> status to false
                Skip -> status.copy(skippedAny = true) to true
                else -> status.copy(status = subRes) to true
            }
        }

        when (val stat = update.status) {
            Failure -> FullResult.Failure
            is TypeArgUpdate -> FullResult.Update(stat)
            is Continue-> {
                if (update.skippedAny) {
                    FullResult.Failure
                } else {
                    FullResult.Success
                }
            }
        }
    }
}

fun fitsQuery(query: QueryType, function: FunctionObj): MatchingContext? {
    return query.inputParameters.allPermutations().asSequence().mapNotNull { inputsOrdered ->
        fitsOrderedQuery(query.copy(inputParameters = inputsOrdered), function)
    }.firstOrNull()
}
