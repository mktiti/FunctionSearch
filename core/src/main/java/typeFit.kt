import InheritanceLogic.*
import Type.GenericType
import Type.GenericType.DynamicAppliedType
import Type.GenericType.TypeTemplate
import Type.NonGenericType
import Type.NonGenericType.DirectType
import Type.NonGenericType.StaticAppliedType

enum class InheritanceLogic {
    INVARIANCE, COVARIANCE, CONTRAVARIANCE
}

sealed class ContextParam

class TypeContext(
    val typeParams: List<ContextParam>
)

sealed class SignatureContext(
    val context: TypeContext,
    val params: List<Type>
) {
    class FunctionContext(
        context: TypeContext,
        params: List<Type>
    ) : SignatureContext(context, params)

    class QueryContext(
        context: TypeContext,
        params: List<Type>
    ) : SignatureContext(context, params)
}

sealed class ContextualParameter {

    abstract val type: Type

    class ConcreteParam(
        override val type: NonGenericType
    ) : ContextualParameter()

    class DynamicParam(
        override val type: DynamicAppliedType,
        val params: TypeParameter
    ) : ContextualParameter()

}

data class FitResult(
    val newQueryContext: ContextualParameter,
    val newFunctionContext: ContextualParameter
)

fun canBeSubstituted(argType: Type, substitution: Type, variance: InheritanceLogic): Boolean {
    if (argType is GenericType) {
        TODO("Generic Type substitution")
    }

    return if (argType.info == substitution.info) {
        when (argType) {
            is DirectType -> true
            is StaticAppliedType -> {
                when (substitution) {
                    is StaticAppliedType -> {
                        argType.typeArgs.zipIfSameLength(substitution.typeArgs)?.all { (atp, stp) ->
                            canBeSubstituted(atp, stp, INVARIANCE)
                        } ?: false
                    }
                    is DynamicAppliedType -> TODO()
                    is DirectType -> false
                    is TypeTemplate -> false
                }
            }
            is GenericType -> TODO() // Smart casting bug
        }
    } else {
        when (variance) {
            INVARIANCE -> false
            COVARIANCE -> {
                substitution.superTypes.any { superType ->
                    canBeSubstituted(argType, superType.type, variance)
                }
            }
            CONTRAVARIANCE -> {
                // TODO - probably wrong ?
                argType.superTypes.any { superType ->
                    canBeSubstituted(superType.type, substitution, variance)
                }
                // TODO("Contravariance substitution fitting")
            }
        }
    }
}

fun fitsQuery(query: TypeSignature, function: Function): TypeSignature? {
    val queryContext: MutableList<TypeParameter> = query.typeParameters.toMutableList()
    val functionContext: MutableList<TypeParameter> = function.signature.typeParameters.toMutableList()

    val result = function.signature.inputParameters.zipIfSameLength(query.inputParameters)?.forEach { (funPar, queryPar) ->
        if (!canBeSubstituted(funPar.second, queryPar.second, COVARIANCE)) {
            return null
        }
    } ?: return null

    if (!canBeSubstituted(function.signature.output, query.output, CONTRAVARIANCE)) {
        return null
    }

    return query
}
