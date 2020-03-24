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

/*
data class ContextualParameter<out A : ApplicationParameter>(
    val context: List<TypeParameter>,
    val param: A
)
 */

/*
sealed class ContextualParameter(
    val context: TypeContext
) {

    abstract val type: Type


    class ConcreteParam(
        context: TypeContext,
        override val type: NonGenericType
    ) : ContextualParameter(context)

    class DynamicParam(
        context: TypeContext,
        override val type: DynamicAppliedType,
        val params: TypeParameter
    ) : ContextualParameter(context)
}
 */

data class FitContext(
    val argCtx: List<TypeParameter>,
    val subCtx: List<TypeParameter>
)
/*
typealias FitResult = Map<Int, Type.NonGenericType>

private val emptyContext = FitContext(emptyList(), emptyList())

fun subSatArgs(funArg: StaticAppliedType, queryArg: StaticAppliedType): Boolean {
    return funArg.typeArgs.zipIfSameLength(queryArg.typeArgs)?.all { (atp, stp) ->
        canBeSubstituted(emptyContext, wrap(atp), wrap(stp), INVARIANCE) != null
    } ?: false
}

fun subStaticParam(
    context: FitContext,
    argPar: StaticAppliedType,
    subPar: TypeSubstitution<*>,
    variance: InheritanceLogic
): Boolean {
    return when (subPar) {
        is StaticTypeSubstitution -> {
            when (subPar.type) {
                is DirectType -> false
                is StaticAppliedType -> subSatArgs(argPar, subPar.type)
            }
        }
        is DynamicTypeSubstitution -> TODO()
    }
}

fun subDatMappingWithSat(
    context: FitContext,
    argParam: ApplicationParameter,
    subArg: Type.NonGenericType
): FitResult? {
    return when (argParam) {
        is ParamSubstitution -> {
            val referencedParam: TypeParameter = context.argCtx.getOrNull(argParam.param) ?: return null

            if (referencedParam.bounds.contains(subArg)) {
                context.copy(argCtx = context.argCtx.updatedCopy(argParam.param, subArg))
            } else {
                null
            }

            // referencedParam.bounds.contains(argPar)
        }
        is DynamicTypeSubstitution -> TODO()
        is StaticTypeSubstitution -> {
            canBeSubstituted(context.argCtx, argParArg, context.subCtx, wrap(subParArg), INVARIANCE)
        }
    }
}

fun subDynamicParam(
    context: FitContext,
    argPar: DynamicAppliedType,
    subPar: TypeSubstitution<*>,
    variance: InheritanceLogic
): FitContext? {
    fun Boolean?.sameCtx(): FitContext? = if (this == true) context else null

    return when (subPar) {
        is StaticTypeSubstitution -> {
            when (subPar.type) {
                is DirectType -> null
                is StaticAppliedType -> {


                    argPar.typeArgMapping.zipIfSameLength(subPar.type.typeArgs)?.all { (argParArg, subParArg) ->
                        when (argParArg) {
                            is ParamSubstitution -> context.argCtx.getOrNull(argParArg.param)?.bounds?.contains(argPar)?.sameCtx()
                            is DynamicTypeSubstitution -> TODO()
                            is StaticTypeSubstitution -> canBeSubstituted(context.argCtx, argParArg, context.subCtx, wrap(subParArg), INVARIANCE)
                        }
                    } ?: null

                    null
                }
            }
        }
        is DynamicTypeSubstitution -> TODO("DAT function parameter (${argPar.fullName}), DAT query parameter ($subPar) mapping")
    }
}

fun subSameBase(
    context: FitContext,
    argPar: TypeSubstitution<*>,
    subPar: TypeSubstitution<*>,
    variance: InheritanceLogic
): FitResult? {
    return when (argPar) {
        is StaticTypeSubstitution -> {
            when (argPar.type) {
                is DirectType -> null
                is StaticAppliedType -> if (subStaticParam(context, argPar.type, subPar, variance)) context else null
            }
        }
        is DynamicTypeSubstitution -> subDynamicParam(context, argPar.type, subPar, variance)
    }
}

fun canBeSubstituted(
    context: FitContext,
    argPar: ApplicationParameter,
    subPar: ApplicationParameter,
    variance: InheritanceLogic
): FitResult? {
    return when (argPar) {
        is ParamSubstitution -> TODO("Top level function param substitution")
        is TypeSubstitution<*> -> {
            when (subPar) {
                is ParamSubstitution -> TODO("Top level substitute param substitution")
                is TypeSubstitution<*> -> {
                    if (argPar.type.info == subPar.type.info) {
                        subSameBase(context, argPar, subPar, INVARIANCE)
                    } else {
                        when (variance) {
                            INVARIANCE -> null
                            COVARIANCE -> {
                                subPar.type.superTypes.asSequence().mapNotNull { superType ->
                                    canBeSubstituted(context, argPar, wrap(superType.type), variance)
                                }.firstOrNull()
                            }
                            CONTRAVARIANCE -> {
                                // TODO - probably wrong ?
                                argPar.type.superTypes.asSequence().mapNotNull { superType ->
                                    canBeSubstituted(context, wrap(superType.type), subPar, variance)
                                }.firstOrNull()
                            }
                        }
                    }
                }
            }
        }
    }
}
*/
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
                is ParamSubstitution -> FunTypeArgUpdate(argPar.param, subPar)
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
                is StaticTypeSubstitution -> FunTypeArgUpdate(argPar.param, subPar.type)
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
/*
sealed class TypeParamUpdate {

    data class Updated(val type: Type.NonGenericType) : TypeParamUpdate()

    data class Kept(val param: TypeParameter) : TypeParamUpdate()

}

fun contextToParams(context: List<TypeParameter>): List<ApplicationParameter> {
    return context.mapIndexed { i, _ -> ParamSubstitution(i) }
}

fun contextUpdateToParams(context: List<TypeParamUpdate>): List<ApplicationParameter> {
    return context.map { param ->
        when (param) {
            is Updated -> StaticTypeSubstitution(param.type)
            is Kept -> TODO()
        }
    }
}

fun updateContext(oldCtx: List<TypeParameter>, change: FitResult): Pair<List<TypeParamUpdate>, List<ApplicationParameter>> {
    return oldCtx.foldIndexed(ArrayList<TypeParamUpdate>(oldCtx.size) to ArrayList(oldCtx.size)) { i, data, param ->
        data.also { (prevParams, aps) ->
            when (val newVal = change[i]) {
                null -> {
                    aps += ParamSubstitution(prevParams.size)
                    prevParams += Kept(param.apply(aps))
                }
                else -> {
                    aps += StaticTypeSubstitution(newVal)
                    prevParams += Updated(newVal)
                }
            }
        }
    }
}

fun trimUpdated(updated: List<TypeParamUpdate>): List<TypeParameter> = updated.mapNotNull {
    when (it) {
        is Updated -> null
        is Kept -> it.param
    }
}
 */
/*
fun updateParam(param: ApplicationParameter, oldCtx: List<TypeParameter>, newCtx: List<TypeParameter>, change: FitResult): ApplicationParameter {
    return when (param) {
        is ParamSubstitution -> {
            when (val newParam = change[param.param]) {
                null -> ParamSubstitution(param.param - change.keys.count { it < param.param })
                else -> StaticTypeSubstitution(newParam)
            }
        }
        is DynamicTypeSubstitution -> {
            when (val applied = param.type.forceApply(emptyList())) {
                is Type.NonGenericType -> StaticTypeSubstitution(applied)
                is DynamicAppliedType -> DynamicTypeSubstitution(applied)
            }
        }
        is StaticTypeSubstitution -> param
    }
}
 */

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

    constructor(query: TypeSignature, function: Function) : this(
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

fun fitsQuery(query: TypeSignature, function: Function): MatchingContext? {
    /*
    data class Pairing<T>(
        val funVal: T,
        val queryVal: T
    )

    data class ParamPair(
        val params: Pairing<ApplicationParameter>,
        val variance: InheritanceLogic
    ) {
        constructor(
            funPar: ApplicationParameter,
            queryPar: ApplicationParameter,
            variance: InheritanceLogic
        ) : this(Pairing(funPar, queryPar), variance)
    }
     */
    /*
    val funTypePars = function.signature.typeParameters
    val queryTypePars = query.typeParameters

    val inParamPairs = function.signature.inputParameters.zipIfSameLength(query.inputParameters) ?: return null
    val inputPairings = inParamPairs.map { (ap, sp) ->
        ParamPair(ap.second, sp.second, COVARIANCE)
    }
    val outputPairing = ParamPair(function.signature.output, query.output, CONTRAVARIANCE)

    val allPairings: List<ParamPair> = ArrayList(inputPairings + outputPairing)

     */

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

    /*
    var matchingContext = MatchingContext(query, function)
    val result: FullResult = doWhile {
        val updated = matchingContext.pairings.mapNotNull { (funArg, queryArg, variance) ->
            subAnyWithAny(matchingContext.context, funArg, queryArg, variance)
        }.filter { it !is ConstraintsKept }.firstOrNull() ?: ConstraintsKept

        when (updated) {
            Failure -> FullResult.Failure
            ConstraintsKept -> FullResult.Success(matchingContext)
            is TypeArgUpdate -> {
                matchingContext = matchingContext.apply(updated)
                null
            }
        }
    }

    return matchingContext
     */
/*
    var workPairs: List<ParamPair> = ArrayList(inputPairings + outputPairing)
    var context = FitContext(funTypePars, queryTypePars)
    workPairs.forEach { (params, variance) ->
        val (funPar, queryPar) = params
        val fitRes: FitResult = canBeSubstituted(context, funPar, queryPar, variance) ?: return null

        val (updated, aps) = updateContext(context.argCtx, fitRes)

        workPairs = workPairs.map { data ->
            val (updateParams, updateVariance) = data
            val (updateFunPar, updateQueryPar) = updateParams

            val updatedFunPar: ApplicationParameter = when (updateFunPar) {
                is ParamSubstitution -> {
                    val referenced = updateFunPar.param
                    when (val newParam = fitRes[referenced]) {
                        null -> ParamSubstitution(referenced - fitRes.keys.count { it < referenced })
                        else -> StaticTypeSubstitution(newParam)
                    }
                }
                is DynamicTypeSubstitution -> {
                    when (val applied = updateFunPar.type.forceApply(emptyList())) {
                        is Type.NonGenericType -> StaticTypeSubstitution(applied)
                        is DynamicAppliedType -> DynamicTypeSubstitution(applied)
                    }
                }
                is StaticTypeSubstitution -> updateFunPar
            }

            data.copy(params = params.copy(funVal = updatedFunPar))
        }

        context = context.copy(argCtx = trimUpdated(updated))
    }

    val result = workPairs.map { it.params.queryVal }

    return TypeSignature.GenericSignature(
        typeParameters = context.argCtx,
        inputParameters = query.inputParameters.map { it.first }.zip(result),
        output = result.last()
    )
 */
}
