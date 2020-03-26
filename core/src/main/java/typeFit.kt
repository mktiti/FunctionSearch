import ApplicationParameter.ParamSubstitution
import ApplicationParameter.TypeSubstitution.Companion.wrap
import ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import InheritanceLogic.*
import SubResult.Failure
import SubResult.Success.ConstraintsKept
import SubResult.Success.TypeArgUpdate
import SubResult.Success.TypeArgUpdate.FunTypeArgUpdate
import SubResult.Success.TypeArgUpdate.QueryTypeArgUpdate
import Type.DynamicAppliedType

enum class InheritanceLogic {
    INVARIANCE, COVARIANCE, CONTRAVARIANCE
}

data class FitContext(
    val argCtx: List<TypeParameter>,
    val subCtx: List<TypeParameter>
)

sealed class SubResult {
    object Failure : SubResult()

    sealed class Success : SubResult() {

        object ConstraintsKept : Success()

        sealed class TypeArgUpdate(val arg: Int, val type: Type.NonGenericType) : Success() {

            class FunTypeArgUpdate(arg: Int, type: Type.NonGenericType) : TypeArgUpdate(arg, type)

            class QueryTypeArgUpdate(arg: Int, type: Type.NonGenericType) : TypeArgUpdate(arg, type)

        }

    }
}

fun subStaticWithStatic(
    argPar: Type.NonGenericType,
    subPar: Type.NonGenericType,
    variance: InheritanceLogic
): Boolean {
    return if (argPar.info == subPar.info) {
        argPar.typeArgs.zipIfSameLength(subPar.typeArgs)?.all { (argPar, subPar) ->
            subStaticWithStatic(argPar, subPar, INVARIANCE)
        } ?: false
    } else {
        when (variance) {
            INVARIANCE -> false
            COVARIANCE -> {
                subPar.superTypes.asSequence().mapNotNull { superType ->
                    subStaticWithStatic(argPar, superType.type, variance)
                }.any()
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argPar.superTypes.asSequence().map { superType ->
                    subStaticWithStatic(superType.type, subPar, variance)
                }.any()
            }
        }
    }
}

fun subTypeSubsWithStatic(
    ctx: List<TypeParameter>,
    arg: ParamSubstitution,
    type: Type.NonGenericType
): SubResult {
    val referenced = ctx.getOrNull(arg.param) ?: return Failure
    return if (referenced.fits(type)) {
        FunTypeArgUpdate(arg.param, type)
    } else {
        Failure
    }
}

fun subDynamicWithStatic(
    argCtx: List<TypeParameter>,
    argPar: DynamicAppliedType,
    subPar: Type.NonGenericType,
    variance: InheritanceLogic
): SubResult {
    fun Boolean.asResult() = if (this) ConstraintsKept else Failure

    fun Sequence<SubResult>.realResult(): SubResult
        = filter { it is Failure || it is TypeArgUpdate }.firstOrNull() ?: ConstraintsKept

    return if (argPar.info == subPar.info) {
        val zipped = argPar.typeArgMapping.zipIfSameLength(subPar.typeArgs) ?: return Failure

        zipped.asSequence().map { (argPar, subPar) ->
            when (argPar) {
                is ParamSubstitution -> subTypeSubsWithStatic(argCtx, argPar, subPar)
                is DynamicTypeSubstitution -> TODO()
                is StaticTypeSubstitution -> {
                    subStaticWithStatic(argPar.type, subPar, INVARIANCE).asResult()
                }
            }
        }.realResult()
    } else {
        when (variance) {
            INVARIANCE -> Failure
            COVARIANCE -> {
                subPar.superTypes.asSequence().map { superType ->
                    subDynamicWithStatic(argCtx, argPar, superType.type, variance)
                }.realResult()
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argPar.superTypes.asSequence().map { superType ->
                    when (val stt = superType.type) {
                        is Type.NonGenericType -> subStaticWithStatic(stt, subPar, variance).asResult()
                        is DynamicAppliedType -> subDynamicWithStatic(argCtx, stt, subPar, variance)
                    }
                }.realResult()
            }
        }
    }
}

// TODO refactor with above
fun subStaticWithDynamic(
    argPar: Type.NonGenericType,
    subCtx: List<TypeParameter>,
    subPar: DynamicAppliedType,
    variance: InheritanceLogic
): SubResult {
    fun Boolean.asResult() = if (this) ConstraintsKept else Failure

    fun Sequence<SubResult>.realResult(): SubResult
            = filter { it is Failure || it is TypeArgUpdate }.firstOrNull() ?: ConstraintsKept

    return if (argPar.info == subPar.info) {
        val zipped = argPar.typeArgs.zipIfSameLength(subPar.typeArgMapping) ?: return Failure

        zipped.asSequence().map { (argPar, subPar) ->
            when (subPar) {
                is ParamSubstitution -> QueryTypeArgUpdate(subPar.param, argPar)
                is DynamicTypeSubstitution -> TODO()
                is StaticTypeSubstitution -> {
                    subStaticWithStatic(argPar, subPar.type, INVARIANCE).asResult()
                }
            }
        }.realResult()
    } else {
        when (variance) {
            INVARIANCE -> Failure
            COVARIANCE -> {
                argPar.superTypes.asSequence().map { superType ->
                    subStaticWithDynamic(superType.type, subCtx, subPar, variance)
                }.realResult()
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                subPar.superTypes.asSequence().map { superType ->
                    when (val stt = superType.type) {
                        is Type.NonGenericType -> subStaticWithStatic(argPar, stt, variance).asResult()
                        is DynamicAppliedType -> subStaticWithDynamic(argPar, subCtx, stt, variance)
                    }
                }.realResult()
            }
        }
    }
}

fun subAnyWithAny(
    context: FitContext,
    argPar: ApplicationParameter,
    subPar: ApplicationParameter,
    variance: InheritanceLogic
): SubResult {
    return when (argPar) {
        is ParamSubstitution -> {
            when (subPar) {
                is ParamSubstitution -> TODO()
                is DynamicTypeSubstitution -> TODO()
                is StaticTypeSubstitution -> subTypeSubsWithStatic(context.argCtx, argPar, subPar.type)
            }
        }
        is DynamicTypeSubstitution -> {
            when (subPar) {
                is ParamSubstitution -> TODO()
                is DynamicTypeSubstitution -> TODO()
                is StaticTypeSubstitution -> subDynamicWithStatic(context.argCtx, argPar.type, subPar.type, COVARIANCE)
            }
        }
        is StaticTypeSubstitution -> {
            when (subPar) {
                is ParamSubstitution -> QueryTypeArgUpdate(subPar.param, argPar.type)
                is DynamicTypeSubstitution -> subStaticWithDynamic(argPar.type, context.subCtx, subPar.type, COVARIANCE)
                is StaticTypeSubstitution -> {
                    if (subStaticWithStatic(argPar.type, subPar.type, variance)) ConstraintsKept else Failure
                }
            }
        }
    }
}

sealed class FullResult {
    object Failure : FullResult()

    object Success : FullResult()

    data class Continue(val update: TypeArgUpdate) : FullResult()
}

tailrec fun transformContext(initial: MatchingContext, code: (context: MatchingContext) -> FullResult): MatchingContext? {
    return when (val midResult = code(initial)) {
        is FullResult.Failure -> null
        is FullResult.Success -> initial
        is FullResult.Continue -> transformContext(initial.apply(midResult.update), code)
    }
}

data class SignatureContext(
    val typeParams: List<TypeParameter>,
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

    fun apply(update: TypeArgUpdate): SignatureContext {
        val updatedTypeParams = typeParams.filterIndexed { i, _ -> i != update.arg }
        val applyTypeParams: List<ApplicationParameter> = typeParams.mapIndexed { i, typePar ->
            when {
                i < update.arg -> ParamSubstitution(i)
                i == update.arg -> StaticTypeSubstitution(update.type)
                else -> {
                    // TODO update type arg
                    ParamSubstitution(i - 1)
                }
            }
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
                is DynamicTypeSubstitution -> wrap(param.type.forceApply(applyTypeParams))
                is StaticTypeSubstitution -> param
            }
        }

        return SignatureContext(updatedTypeParams, updatedParams)
    }
}

data class ParamPair(
    val funParam: ApplicationParameter,
    val queryParam: ApplicationParameter,
    val variance: InheritanceLogic
)

data class MatchingContext(
    val funCtx: SignatureContext,
    val queryCtx: SignatureContext,
    val varianceCtx: List<InheritanceLogic>
) {

    val context = FitContext(
        argCtx = funCtx.typeParams,
        subCtx = queryCtx.typeParams
    )

    val pairings: Sequence<ParamPair>
        get() = funCtx.parameters.zipIfSameLength(queryCtx.parameters)?.zipIfSameLength(varianceCtx)?.map { (params, variance) ->
            ParamPair(
                funParam = params.first,
                queryParam = params.second,
                variance = variance
            )
        }?.asSequence()!!

    constructor(query: TypeSignature, function: FunctionObj) : this(
        funCtx = SignatureContext.fromTypeSignature(function.signature),
        queryCtx = SignatureContext.fromTypeSignature(query),
        varianceCtx = nList(COVARIANCE, query.inputParameters.size) + CONTRAVARIANCE
    )

    fun apply(update: TypeArgUpdate): MatchingContext {
        return when (update) {
            is FunTypeArgUpdate -> copy(funCtx = funCtx.apply(update))
            is QueryTypeArgUpdate -> copy(queryCtx = queryCtx.apply(update))
        }
    }

    override fun toString(): String {
        return "Type signature match: ${if (funCtx.typeParams.isNotEmpty()) "" else funCtx.typeParams.genericString()} (${funCtx.parameters.dropLast(1).joinToString()}) -> ${funCtx.parameters.last()}"
    }

}

fun fitsQuery(query: TypeSignature, function: FunctionObj): MatchingContext? {
    return transformContext(MatchingContext(query, function)) { matchingCtx ->
        val update = matchingCtx.pairings.mapNotNull { (funArg, queryArg, variance) ->
            subAnyWithAny(matchingCtx.context, funArg, queryArg, variance)
        }.filter { it !is ConstraintsKept }.firstOrNull() ?: ConstraintsKept

        when (update) {
            Failure -> FullResult.Failure
            ConstraintsKept -> FullResult.Success
            is TypeArgUpdate -> FullResult.Continue(update)
        }
    }
}
