package com.mktiti.fsearch.core.util.show

import com.mktiti.fsearch.core.fit.FittingMap
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.util.genericString

class JavaTypeStringResolver(
        private val infoRepo: JavaInfoRepo
) : TypeStringResolver {

    private fun resolveName(info: MinimalInfo, typeArgs: String?): String {
        return if (info == infoRepo.arrayType) {
            "$typeArgs[]"
        } else {
            val name = when {
                !infoRepo.isInternal(info) -> info.fullName
                info == infoRepo.voidType -> "()"
                else -> info.simpleName
            }

            if (typeArgs == null) {
                name
            } else {
                "$name<$typeArgs>"
            }
        }
    }

    override fun resolveName(info: CompleteMinInfo.Static): String {
        val args = if (info.args.isEmpty()) {
            null
        } else {
            info.args.joinToString(separator = ", ", transform = this::resolveName)
        }

        return resolveName(info.base, args)
    }

    private fun resolveMapSub(arg: ApplicationParameter.Substitution, typeParams: List<String>): String = when (arg) {
        is ParamSubstitution -> typeParams.getOrNull(arg.param) ?: "#${arg.param}"
        SelfSubstitution -> "\$SELF"
        is TypeSubstitution<*, *> -> resolveName(arg.holder.info, typeParams)
    }

    override fun resolveName(info: CompleteMinInfo.Dynamic, typeParams: List<String>): String {
        val args = info.args.joinToString(separator = ", ") { arg ->
            when (arg) {
                is ApplicationParameter.BoundedWildcard -> {
                    when (arg.direction) {
                        BoundDirection.LOWER -> "? super "
                        BoundDirection.UPPER -> "? extends "
                    } + resolveMapSub(arg.param, typeParams)
                }
                is ApplicationParameter.Substitution -> resolveMapSub(arg, typeParams)
            }
        }

        return resolveName(info.base, args)
    }

    private fun resolveTypeName(type: Type<*>): String = resolveName(type.completeInfo)

    override fun resolveSemiName(semi: SemiType): String = when (semi) {
        is TypeTemplate -> {
            val appliedInfo = semi.info.dynamicComplete(semi.typeParams.mapIndexed { i, _ -> ParamSubstitution(i) })
            resolveName(appliedInfo, semi.typeParams.map { it.sign })
        }
        is Type<*> -> resolveTypeName(semi)
        else -> "Unknown semi type $semi"
    }

    override fun resolveFittingMap(result: FittingMap): String = buildString {
        append(result.typeParamMapping.genericString { (param, type) -> "${param.sign} = ${resolveTypeName(type)}" })
        val inputPairs = result.funSignature.inputParameters.zip(result.orderedQuery.inputParameters)
        val paramMap = inputPairs.joinToString(prefix = " (", separator = ", ", postfix = ") -> ") { (funIn, queryIn) ->
            "${funIn.first}: ${resolveTypeName(queryIn)}"
        }
        append(paramMap)
        append(resolveTypeName(result.orderedQuery.output))
    }

    private fun resolveTypeParams(typeParams: List<TypeParameter>): String {
        val names = typeParams.map { it.sign }
        return typeParams.genericString { param ->
            buildString {
                append(param.sign)
                append(" : ")
                val bounds = param.bounds.upperBounds.joinToString { bound ->
                    resolveMapSub(bound, names)
                }
                append(bounds)
            }
        }
    }

    override fun resolveFun(function: FunctionObj): String = buildString {
        append("fun ")
        function.signature.apply {
            if (typeParameters.isNotEmpty()) {
                append(resolveTypeParams(typeParameters))
                append(" ")
            }
        }
        function.info.apply {
            if (fileName.isNotBlank()) {
                append(fileName)
                append("::")
            }
            append(name)
        }
        function.signature.apply {
            val typeParams = typeParameters.map { it.sign }

            val ins = if (inputParameters.isEmpty()) {
                "(() -> "
            } else {
                inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ") { (name, param) ->
                    name + ": " + resolveMapSub(param, typeParams)
                }
            }
            append(ins)
            append(resolveMapSub(output, typeParams))
        }
    }

    override fun resolveQuery(query: QueryType): String = buildString {
        val ins = if (query.inputParameters.isEmpty()) {
            "(() -> "
        } else {
            query.inputParameters.joinToString(prefix = "(", separator = ", ", postfix = " -> ") { inParam ->
                resolveName(inParam.completeInfo)
            }
        }
        append(ins)
        append(resolveName(query.output.completeInfo))
        append(")")
    }

}