package com.mktiti.fsearch.parser.intermediate.parse

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.intermediate.*
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import com.mktiti.fsearch.util.repeat
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class DefaultFunctionSignatureInfoBuilder(
        private val infoRepo: JavaInfoRepo,
        private val signatureParser: JavaSignatureInfoParser = DefaultSignatureInfoParser(infoRepo)
) : JavaFunctionSignatureInfoBuilder {

    companion object {
        private fun CompleteMinInfo.Static.wrap() = TypeParamInfo.Sat(this)
    }

    private fun parseStaticReturnType(value: SignatureParser.ReturnTypeContext): CompleteMinInfo.Static? = when (val type = value.javaTypeSignature()) {
        null -> infoRepo.voidType.complete()
        else -> signatureParser.parseStaticJavaType(type)
    }

    private fun parseReturnType(value: SignatureParser.ReturnTypeContext, typeParams: List<String>): TypeParamInfo {
        return parseStaticReturnType(value)?.wrap() ?: signatureParser.parseJavaType(value.javaTypeSignature(), typeParams, selfParamName = null)
    }

    private data class ParseBase(
            val signatureCtx: SignatureParser.MethodSignatureContext,
            val typeParams: List<TemplateTypeParamInfo>
    )

    private fun <T> List<T>.withNames(sigCtx: SignatureParser.MethodSignatureContext, paramNames: List<String>?): List<Pair<String, T>> = if (paramNames == null) {
        mapIndexed { i, param -> "\$arg$i" to param }
    } else {
        val missingCount = maxOf(0, sigCtx.javaTypeSignature().size - paramNames.size)
        val initNames: List<String?> = paramNames + null.repeat(missingCount)
        zip(initNames).mapIndexed { i, (param, name) ->
            (name ?: "\$arg$i") to param
        }
    }

    private fun parseStaticArgs(parseBase: ParseBase, paramNames: List<String>?, implicitThis: CompleteMinInfo.Static?): FunSignatureInfo.Direct? {
        val (signatureCtx, typeParams) = parseBase
        return if (typeParams.isEmpty()) {
            signatureCtx.javaTypeSignature().withNames(signatureCtx, paramNames).map { (name, param) ->
                name to (signatureParser.parseStaticJavaType(param) ?: return@map null)
            }.liftNull()?.let { inputs ->
                val allInputs = if (implicitThis == null) {
                    emptyList()
                } else {
                    listOf("\$this" to implicitThis)
                } + inputs

                FunSignatureInfo.Direct(
                        inputs = allInputs,
                        output = parseStaticReturnType(signatureCtx.returnType()) ?: return@let null
                )
            }
        } else {
            null
        }
    }

    private fun parseDynamicArgs(parseBase: ParseBase, paramNames: List<String>?, implicitThis: TypeParamInfo?): FunSignatureInfo.Generic {
        val (signatureCtx, typeParams) = parseBase
        val typeParamNames = typeParams.map { it.sign }
        val inputs = signatureCtx.javaTypeSignature().withNames(signatureCtx, paramNames).map { (name, param) ->
            name to signatureParser.parseJavaType(param, typeParamNames, selfParamName = null)
        }

        val allInputs = if (implicitThis == null) {
            emptyList()
        } else {
            listOf("\$this" to implicitThis)
        } + inputs

        return FunSignatureInfo.Generic(
                typeParams = typeParams,
                inputs = allInputs,
                output = parseReturnType(signatureCtx.returnType(), typeParamNames)
        )
    }

    private fun parseFunctionBase(signature: String, typeLevelTypeParams: List<TemplateTypeParamInfo>): ParseBase {
        val lexer = SignatureLexer(CharStreams.fromString(signature))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = SignatureParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val signatureCtx = parser.methodSignature()
        val typeLevelTpNames = typeLevelTypeParams.map { it.sign }
        val typeParams = typeLevelTypeParams + signatureParser.parseTypeParams(signatureCtx.typeParameters(), typeLevelTpNames)

        return ParseBase(signatureCtx, typeParams)
    }

    override fun parseDirectSam(paramNames: List<String>?, signature: String): FunSignatureInfo.Direct? {
        val parseBase = try {
            parseFunctionBase(signature, emptyList())
        } catch (typeError: UndeclaredTypeArgReference) {
            // TODO log or fix
            return null
        }

        return if (parseBase.typeParams.isEmpty()) {
            parseStaticArgs(parseBase, paramNames, null)?.let { info ->
                FunSignatureInfo.Direct(
                        inputs = info.inputs,
                        output = info.output
                )
            }
        } else {
            null
        }
    }

    override fun parseGenericSam(paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TemplateTypeParamInfo>): FunSignatureInfo.Generic? {
        val parseBase = parseFunctionBase(signature, implicitThisTps)
        val parsed = parseDynamicArgs(parseBase, paramNames, null)

        return if (parsed.typeParams.size == implicitThisTps.size) {
            FunSignatureInfo.Generic(
                    typeParams = parsed.typeParams,
                    inputs = parsed.inputs,
                    output = parsed.output
            )
        } else {
            null
        }
    }

    override fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TemplateTypeParamInfo>): FunSignatureInfo.Generic {
        val parseBase = parseFunctionBase(signature, implicitThisTps)

        val thisDat = DatInfo(
                template = implicitThis,
                args = implicitThisTps.indices.map { TypeParamInfo.Param(it) }
        )

        return parseDynamicArgs(parseBase, paramNames, TypeParamInfo.Dat(thisDat))
    }

    /*override fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: TypeTemplate): FunSignatureInfo<*> {
        return parseTemplateFunction(name, paramNames, signature, implicitThis.info, implicitThis.typeParams)
    }
     */

    override fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo): FunSignatureInfo<*> {
        val parseBase = parseFunctionBase(signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, implicitThis.complete())
                ?: parseDynamicArgs(parseBase, paramNames, TypeParamInfo.Sat(implicitThis.complete()))
    }

    override fun parseDirectConstructor(type: MinimalInfo, signature: String, paramNames: List<String>?): FunSignatureInfo<*> {
        val parseBase = parseFunctionBase(signature, emptyList())
        return when (val static = parseStaticArgs(parseBase, paramNames, implicitThis = null)) {
            null -> {
                val generic = parseDynamicArgs(parseBase, paramNames, implicitThis = null)
                FunSignatureInfo.Generic(
                        typeParams = generic.typeParams,
                        inputs = generic.inputs,
                        output = TypeParamInfo.Direct(type)
                )
            }
            else -> {
                FunSignatureInfo.Direct(
                        inputs = static.inputs,
                        output = type.complete()
                )
            }
        }
    }

    override fun parseTemplateConstructor(type: MinimalInfo, typeParams: List<TemplateTypeParamInfo>, signature: String, paramNames: List<String>?): FunSignatureInfo.Generic {
        val parseBase = parseFunctionBase(signature, emptyList()).copy(
                typeParams = typeParams
        )
        val parsed = parseDynamicArgs(parseBase, paramNames, implicitThis = null)

        val thisDat = DatInfo(
                template = type,
                args = typeParams.indices.map { TypeParamInfo.Param(it) }
        )

        return FunSignatureInfo.Generic(
                typeParams = parsed.typeParams,
                inputs = parsed.inputs,
                output = TypeParamInfo.Dat(thisDat)
        )
    }

    override fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): FunSignatureInfo<*> {
        val parseBase = parseFunctionBase(signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, null) ?: parseDynamicArgs(parseBase, paramNames, null)
    }
}
