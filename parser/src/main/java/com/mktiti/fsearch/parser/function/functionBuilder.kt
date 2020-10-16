package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.UPPER
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.util.forceDynamicApply

interface FunctionBuilder {

    data class ImplicitThis(
            val info: MinimalInfo,
            val isGeneric: Boolean
    )

    fun buildFunction(intermediate: ImSignature, implicitThis: ImplicitThis?): TypeSignature?
}

class JavaFunctionBuilder(
        private val javaRepo: JavaRepo,
        private val typeResolver: TypeResolver
) : FunctionBuilder {

    private fun mapDatParam(param: ImParam.Type, references: List<TypeParameter>, selfRef: String?): TypeSubstitution<*, *>? {
        val template = typeResolver.template(param.info) ?: return null
        val args: List<ApplicationParameter> = if (param.typeArgs.isEmpty()) {
            template.typeParams.map { StaticTypeSubstitution(javaRepo.objectType) }
        } else {
            param.typeArgs.map { mapParam(it, references, selfRef) ?: return null }
        }
        return TypeSubstitution(template.forceDynamicApply(args).holder())
    }

    private fun mapParam(param: ImParam, references: List<TypeParameter>, selfRef: String?): ApplicationParameter? {
        return when (param) {
            ImParam.Wildcard -> TypeSubstitution.unboundedWildcard
            is ImParam.Primitive -> StaticTypeSubstitution(javaRepo.primitive(param.value))
            is ImParam.UpperWildcard -> BoundedWildcard(mapSubstitution(param.param, references, selfRef) ?: return null, UPPER)
            is ImParam.LowerWildcard -> BoundedWildcard(mapSubstitution(param.param, references, selfRef) ?: return null, LOWER)
            is ImParam.Array -> TypeSubstitution(javaRepo.arrayOf(mapParam(param.type, references, selfRef) ?: return null).holder())
            is ImParam.Type -> {
                val info = param.info

                if (param.typeArgs.isEmpty()) {
                    // Direct type
                    val type = typeResolver[info]
                    if (type == null) {
                        mapDatParam(param, references, selfRef)
                    } else {
                        StaticTypeSubstitution(type.holder())
                    }
                } else {
                    // Dat
                    mapDatParam(param, references, selfRef)
                }
            }
            is ImParam.TypeParamRef -> {
                if (selfRef == param.sign) {
                    Substitution.SelfSubstitution
                } else {
                    val index = references.withIndex().find { tp -> param.sign == tp.value.sign.trimStart('$') }?.index
                            ?: error("Invalid type param reference ('${param.sign}')")

                    ParamSubstitution(index)
                }
            }
            ImParam.Void -> StaticTypeSubstitution(javaRepo.voidType)
        }
    }

    private fun mapSubstitution(type: ImParam, references: List<TypeParameter>, selfRef: String?): Substitution? {
        val mapped = mapParam(type, references, selfRef) ?: return null
        return mapped as? Substitution ?: error("Only substitutions allowed")
    }

    private fun buildTypeParam(intermediate: ImTypeParam, references: List<TypeParameter>): TypeParameter? {
        return TypeParameter(
                sign = intermediate.sign,
                bounds = TypeBounds(
                    upperBounds = intermediate.bounds.map { bound ->
                        mapSubstitution(bound, references, intermediate.sign) ?: return null
                    }.toSet()
                )
        )
    }

    override fun buildFunction(intermediate: ImSignature, implicitThis: FunctionBuilder.ImplicitThis?): TypeSignature? {
        val thisTemplate: TypeTemplate? = if (implicitThis != null && implicitThis.isGeneric) {
            typeResolver.template(implicitThis.info) ?: return null
        } else {
            null
        }
        val typeLevelTypeParams = thisTemplate?.typeParams ?: emptyList()

        val explicitTypeParamCount = intermediate.typeParams.size

        val typeParams: List<TypeParameter> = ArrayList<TypeParameter>(explicitTypeParamCount + typeLevelTypeParams.size).apply {
            // Topological sort, may be optimized
            val indexedTypeParams = intermediate.typeParams.withIndex().toMutableList()

            while (indexedTypeParams.isNotEmpty()) {
                val addedSigns = map { it.sign }

                val (index, nonDependent) = indexedTypeParams.find { (_, tp) ->
                    (addedSigns + tp.sign).containsAll(tp.referencedTypeParams)
                } ?: throw RuntimeException("Function type parameters have cyclic dependency")

                this += buildTypeParam(nonDependent, this) ?: return null
                indexedTypeParams.removeIf { it.index == index }
            }

            addAll(typeLevelTypeParams.map { it.copy(sign = "\$${it.sign}") })
        }

        fun param(intermediate: ImParam): Substitution? {
            return mapSubstitution(intermediate, typeParams, null)
        }

        val allInputs: List<Pair<String, Substitution>> = ArrayList<Pair<String, Substitution>>(intermediate.inputs.size).apply {
            if (implicitThis != null) {
                val thisParam = if (thisTemplate == null) {
                    StaticTypeSubstitution(typeResolver[implicitThis.info]?.holder() ?: return null)
                } else {
                    val applied = thisTemplate.dynamicApply(
                            thisTemplate.typeParams.mapIndexed { i, _ ->
                                ParamSubstitution(explicitTypeParamCount + i)
                            }
                    ) ?: return null
                    TypeSubstitution(applied.holder())
                }

                this += ("\$this" to thisParam)
            }

            addAll(intermediate.inputs.mapIndexed { i, (name, p) -> (name ?: "arg\$$i") to (param(p) ?: return null) })
        }

        return TypeSignature.GenericSignature(
            typeParameters = typeParams,
            inputParameters = allInputs,
            output = param(intermediate.output) ?: return null
        )
    }
}
