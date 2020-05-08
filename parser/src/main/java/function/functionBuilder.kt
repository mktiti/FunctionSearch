package function

import ApplicationParameter
import ApplicationParameter.*
import ApplicationParameter.Wildcard.BoundedWildcard.*
import ApplicationParameter.Substitution.*
import ApplicationParameter.Substitution.TypeSubstitution.*
import repo.JavaRepo
import TypeBounds
import TypeParameter
import repo.TypeRepo
import TypeSignature
import TypeTemplate
import forceDynamicApply
import java.lang.RuntimeException

interface FunctionBuilder {
    fun buildFunction(intermediate: ImSignature): TypeSignature?
}

class JavaFunctionBuilder(
        private val artifact: String,
        private val typeRepo: TypeRepo,
        private val javaRepo: JavaRepo
) : FunctionBuilder {

    private fun mapDatParam(param: ImParam.Type, references: List<TypeParameter>): DynamicTypeSubstitution? {
        val template = typeRepo.template(param.info) ?: return null
        val args: List<ApplicationParameter> = if (param.typeArgs.isEmpty()) {
            template.typeParams.map { StaticTypeSubstitution(javaRepo.objectType) }
        } else {
            param.typeArgs.map { mapParam(it, references) ?: return null }
        }
        return DynamicTypeSubstitution(template.forceDynamicApply(args))
    }

    private fun mapParam(param: ImParam, references: List<TypeParameter>): ApplicationParameter? {
        return when (param) {
            ImParam.Wildcard -> Wildcard.Direct
            is ImParam.Primitive -> StaticTypeSubstitution(javaRepo.primitive(param.value))
            is ImParam.UpperWildcard -> UpperBound(mapSubstitution(param.param, references) ?: return null)
            is ImParam.LowerWildcard -> LowerBound(mapSubstitution(param.param, references) ?: return null)
            is ImParam.Array -> DynamicTypeSubstitution(javaRepo.arrayOf(mapParam(param.type, references) ?: return null))
            is ImParam.Type -> {
                val info = param.info

                if (param.typeArgs.isEmpty()) {
                    // Direct type
                    val type = typeRepo[info]
                    if (type == null) {
                        // println("Direct type $info not found, trying raw usage")
                        mapDatParam(param, references)
                    } else {
                        StaticTypeSubstitution(type)
                    }
                } else {
                    // Dat
                    mapDatParam(param, references)
                }
            }
            is ImParam.TypeParamRef -> {
                val index = references.withIndex().find { tp -> param.sign == tp.value.sign }?.index
                        ?: error("Invalid type param reference ('${param.sign}')")

                ParamSubstitution(index)
            }
            ImParam.Void -> StaticTypeSubstitution(javaRepo.voidType)
        }
    }

    private fun mapSubstitution(type: ImParam, references: List<TypeParameter>): Substitution? {
        val mapped = mapParam(type, references) ?: return null
        return mapped as? Substitution ?: error("Only substitutions allowed")
    }

    private fun buildTypeParam(intermediate: ImTypeParam, references: List<TypeParameter>): TypeParameter? {
        return TypeParameter(
                sign = intermediate.sign,
                bounds = TypeBounds(intermediate.bounds.map { mapSubstitution(it, references) ?: return null }.toSet())
        )
    }

    override fun buildFunction(intermediate: ImSignature): TypeSignature? {
        val typeParams: List<TypeParameter> = ArrayList<TypeParameter>(intermediate.typeParams.size).apply {
            // Topological sort, may be optimized
            val indexedTypeParams = intermediate.typeParams.withIndex().toMutableList()
            val added: MutableList<TypeParameter> = ArrayList(intermediate.typeParams.size)

            while (indexedTypeParams.isNotEmpty()) {
                val addedSigns = added.map { it.sign }
                val (index, nonDependent) = indexedTypeParams.find { (_, tp) ->
                    addedSigns.containsAll(tp.referencedTypeParams)
                //} ?: throw RuntimeException("Function type parameters have cyclic dependency")
                } ?: return null

                this += buildTypeParam(nonDependent, added) ?: return null
                indexedTypeParams.removeIf { it.index == index }
            }
        }

        return TypeSignature.GenericSignature(
            typeParameters = typeParams,
            inputParameters = intermediate.inputs.mapIndexed { i, p -> "\$$i" to (mapSubstitution(p, typeParams) ?: return null) },
            output = mapSubstitution(intermediate.output, typeParams) ?: return null
        )
    }
}
