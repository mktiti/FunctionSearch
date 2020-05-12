package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard.LowerBound
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard.UpperBound
import com.mktiti.fsearch.core.util.forceDynamicApply

interface FunctionBuilder {
    fun buildFunction(intermediate: ImSignature): TypeSignature?
}

class JavaFunctionBuilder(
        private val javaRepo: JavaRepo,
        private val typeRepos: Collection<TypeRepo>
) : FunctionBuilder {

    private fun <R : Any> firstFromDeps(mapper: TypeRepo.() -> R?): R?
            = typeRepos.asSequence().mapNotNull { it.mapper() }.firstOrNull()

    private fun direct(info: MinimalInfo) = firstFromDeps { get(info) }
    private fun template(info: MinimalInfo) = firstFromDeps { template(info) }

    private fun mapDatParam(param: ImParam.Type, references: List<TypeParameter>, selfRef: String?): DynamicTypeSubstitution? {
        val template = template(param.info) ?: return null
        val args: List<ApplicationParameter> = if (param.typeArgs.isEmpty()) {
            template.typeParams.map { StaticTypeSubstitution(javaRepo.objectType) }
        } else {
            param.typeArgs.map { mapParam(it, references, selfRef) ?: return null }
        }
        return DynamicTypeSubstitution(template.forceDynamicApply(args))
    }

    private fun mapParam(param: ImParam, references: List<TypeParameter>, selfRef: String?): ApplicationParameter? {
        return when (param) {
            ImParam.Wildcard -> Wildcard.Direct
            is ImParam.Primitive -> StaticTypeSubstitution(javaRepo.primitive(param.value))
            is ImParam.UpperWildcard -> UpperBound(mapSubstitution(param.param, references, selfRef) ?: return null)
            is ImParam.LowerWildcard -> LowerBound(mapSubstitution(param.param, references, selfRef) ?: return null)
            is ImParam.Array -> DynamicTypeSubstitution(javaRepo.arrayOf(mapParam(param.type, references, selfRef) ?: return null))
            is ImParam.Type -> {
                val info = param.info

                if (param.typeArgs.isEmpty()) {
                    // Direct type
                    val type = direct(info)
                    if (type == null) {
                        mapDatParam(param, references, selfRef)
                    } else {
                        StaticTypeSubstitution(type)
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
                    val index = references.withIndex().find { tp -> param.sign == tp.value.sign }?.index
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

    override fun buildFunction(intermediate: ImSignature): TypeSignature? {
        val typeParams: List<TypeParameter> = ArrayList<TypeParameter>(intermediate.typeParams.size).apply {
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
        }

        return TypeSignature.GenericSignature(
            typeParameters = typeParams,
            inputParameters = intermediate.inputs.mapIndexed { i, p -> "\$$i" to (mapSubstitution(p, typeParams, null) ?: return null) },
            output = mapSubstitution(intermediate.output, typeParams, null) ?: return null
        )
    }
}
