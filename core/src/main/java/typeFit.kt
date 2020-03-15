import ApplicationParameter.*
import InheritanceLogic.*
import Type.DynamicAppliedType
import Type.NonGenericType.DirectType
import Type.NonGenericType.StaticAppliedType

enum class InheritanceLogic {
    INVARIANCE, COVARIANCE, CONTRAVARIANCE
}

sealed class SignatureContext(
    val context: List<TypeParameter>,
    val params: List<Type>
) {
    class FunctionContext(
        context: List<TypeParameter>,
        params: List<Type>
    ) : SignatureContext(context, params)

    class QueryContext(
        context: List<TypeParameter>,
        params: List<Type>
    ) : SignatureContext(context, params)
}

data class ContextualParameter<out T : Type>(
    val context: List<TypeParameter>,
    val param: T
)

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

data class FitResult(
    val newQueryContext: ContextualParameter<Type>,
    val newFunctionContext: ContextualParameter<Type>
)

fun subStatic(argCtx: List<TypeParameter>, argParam: StaticAppliedType, subCtx: ContextualParameter<Type>, variance: InheritanceLogic): Boolean {
    return when (subCtx.param) {
        is StaticAppliedType -> {
            argParam.typeArgs.zipIfSameLength(subCtx.param.typeArgs)?.all { (atp, stp) ->
                canBeSubstituted(ContextualParameter(argCtx, atp), subCtx.copy(param = stp), INVARIANCE)
            } ?: false
        }
        is DynamicAppliedType -> TODO()
        is DirectType -> false
    }
}

fun subDynamic(argCtx: List<TypeParameter>, argParam: DynamicAppliedType, subCtx: ContextualParameter<Type>, variance: InheritanceLogic): Boolean {
    return when (subCtx.param) {
        is StaticAppliedType -> {
            argParam.typeArgMapping.zipIfSameLength(subCtx.param.typeArgs)?.all { (argPar, subPar) ->
                when (argPar) {
                    is ParamSubstitution -> argCtx.getOrNull(argPar.param)?.bounds?.contains(argParam) ?: false
                    is DynamicTypeSubstitution -> TODO()
                    is StaticTypeSubstitution -> canBeSubstituted(ContextualParameter(argCtx, argPar.type), subCtx.copy(param = subPar), INVARIANCE)
                }
            } ?: false
        }
        is DynamicAppliedType -> TODO("DAT function parameter (${argParam.fullName}), DAT query parameter (${subCtx.param.fullName}) mapping")
        is DirectType -> false
    }
}

fun subSameBase(argCtx: ContextualParameter<Type>, subCtx: ContextualParameter<Type>, variance: InheritanceLogic): Boolean {
    return when (argCtx.param) {
        is DirectType -> true
        is StaticAppliedType -> subStatic(argCtx.context, argCtx.param, subCtx, variance)
        is DynamicAppliedType -> subDynamic(argCtx.context, argCtx.param, subCtx, variance)
    }
}

fun canBeSubstituted(argCtx: ContextualParameter<Type>, subCtx: ContextualParameter<Type>, variance: InheritanceLogic): Boolean {
    return if (argCtx.param.info == subCtx.param.info) {
        subSameBase(argCtx, subCtx, INVARIANCE)
    } else {
        when (variance) {
            INVARIANCE -> false
            COVARIANCE -> {
                subCtx.param.superTypes.any { superType ->
                    canBeSubstituted(argCtx, subCtx.copy(param = superType.type), variance)
                }
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argCtx.param.superTypes.any { superType ->
                    canBeSubstituted(argCtx.copy(param = superType.type), subCtx, variance)
                }
            }
        }
    }
}

fun fitsQuery(query: TypeSignature, function: Function): TypeSignature? {
    val funPars: MutableList<TypeParameter> = function.signature.typeParameters.toMutableList()
    val queryPars: MutableList<TypeParameter> = query.typeParameters.toMutableList()

    function.signature.inputParameters.zipIfSameLength(query.inputParameters)?.forEach { (funPar, queryPar) ->
        if (!canBeSubstituted(ContextualParameter(funPars, funPar.second), ContextualParameter(queryPars, queryPar.second), COVARIANCE)) {
            return null
        }
    } ?: return null

    if (!canBeSubstituted(ContextualParameter(funPars, function.signature.output), ContextualParameter(queryPars, query.output), CONTRAVARIANCE)) {
        return null
    }

    return query
}
