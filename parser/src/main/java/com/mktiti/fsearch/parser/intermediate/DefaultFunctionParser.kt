package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.Companion
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.forceDynamicApply
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import com.mktiti.fsearch.util.repeat
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class DefaultFunctionParser(
        private val infoRepo: JavaInfoRepo,
        private val signatureParser: JavaSignatureParser = DefaultSignatureParser(infoRepo)
) : JavaSignatureFunctionParser {

    companion object {
        private fun CompleteMinInfo.Static?.wrap(): StaticTypeSubstitution? = this?.let { TypeSubstitution(it.holder()) }
    }

    private fun parseStaticReturnType(value: SignatureParser.ReturnTypeContext): StaticTypeSubstitution? = when (val type = value.javaTypeSignature()) {
        null -> infoRepo.voidType.complete()
        else -> signatureParser.parseStaticJavaType(type)
    }.wrap()

    private fun parseReturnType(value: SignatureParser.ReturnTypeContext, typeParams: List<String>): Substitution {
        return parseStaticReturnType(value) ?: signatureParser.parseJavaType(value.javaTypeSignature(), typeParams)
    }

    private data class ParseBase(
            val signatureCtx: SignatureParser.MethodSignatureContext,
            val typeParams: List<TypeParameter>
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

    private fun parseStaticArgs(parseBase: ParseBase, paramNames: List<String>?, implicitThis: CompleteMinInfo.Static?): TypeSignature.DirectSignature? {
        val (signatureCtx, typeParams) = parseBase
        return if (typeParams.isEmpty()) {
            signatureCtx.javaTypeSignature().withNames(signatureCtx, paramNames).map { (name, param) ->
                name to (signatureParser.parseStaticJavaType(param).wrap() ?: return@map null)
            }.liftNull()?.let { inputs ->
                val allInputs = if (implicitThis == null) {
                    emptyList()
                } else {
                    listOf("\$this" to TypeSubstitution(implicitThis.holder()))
                } + inputs

                TypeSignature.DirectSignature(
                        inputParameters = allInputs,
                        output = parseStaticReturnType(signatureCtx.returnType()) ?: return@let null
                )
            }
        } else {
            null
        }
    }

    private fun parseDynamicArgs(parseBase: ParseBase, paramNames: List<String>?, implicitThis: TypeSubstitution<*, *>?): TypeSignature.GenericSignature {
        val (signatureCtx, typeParams) = parseBase
        val typeParamNames = typeParams.map { it.sign }
        val inputs = signatureCtx.javaTypeSignature().withNames(signatureCtx, paramNames).map { (name, param) ->
            name to signatureParser.parseJavaType(param, typeParamNames)
        }

        val allInputs = if (implicitThis == null) {
            emptyList()
        } else {
            listOf("\$this" to implicitThis)
        } + inputs

        return TypeSignature.GenericSignature(
                typeParameters = typeParams,
                inputParameters = allInputs,
                output = parseReturnType(signatureCtx.returnType(), typeParamNames)
        )
    }

    private fun parseFunctionBase(name: String, paramNames: List<String>?, signature: String, typeLevelTypeParams: List<TypeParameter>): ParseBase {
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
/*
        fun <T> List<T>.withNames(): List<Pair<String, T>> = if (paramNames == null) {
            mapIndexed { i, param -> "\$arg$i" to param }
        } else {
            val missingCount = maxOf(0, signatureCtx.javaTypeSignature().size - paramNames.size)
            val initNames: List<String?> = paramNames + null.repeat(missingCount)
            zip(initNames).mapIndexed { i, (param, name) ->
                (name ?: "\$arg$i") to param
            }
        }

        val staticSignature = if (typeParams.isEmpty()) {
            signatureCtx.javaTypeSignature().withNames().map { (name, param) ->
                name to (signatureParser.parseStaticJavaType(param).wrap() ?: return@map null)
            }.liftNull()?.let { inputs ->
                TypeSignature.DirectSignature(
                        inputParameters = inputs,
                        output = parseStaticReturnType(signatureCtx.returnType()) ?: return@let null
                )
            }
        } else {
            null
        }

        return if (staticSignature != null) {
            staticSignature
        } else {
            val typeParamNames = typeParams.map { it.sign }
            val inputs = signatureCtx.javaTypeSignature().withNames().map { (name, param) ->
                name to signatureParser.parseJavaType(param, typeParamNames)
            }

            TypeSignature.GenericSignature(
                    typeParameters = typeParams,
                    inputParameters = inputs,
                    output = parseReturnType(signatureCtx.returnType(), typeParamNames)
            )
        }
 */
    }

    override fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: TypeTemplate): TypeSignature {
        val parseBase = parseFunctionBase(name, paramNames, signature, implicitThis.typeParams)
        val thisParam = implicitThis.forceDynamicApply(implicitThis.typeParams.mapIndexed { i, _ -> Substitution.ParamSubstitution(i) })
        return parseDynamicArgs(parseBase, paramNames, TypeSubstitution(thisParam.holder()))
    }

    override fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: CompleteMinInfo.Static): TypeSignature {
        val parseBase = parseFunctionBase(name, paramNames, signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, implicitThis) ?: parseDynamicArgs(parseBase, paramNames, TypeSubstitution(implicitThis.holder()))
    }

    override fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature {
        val parseBase = parseFunctionBase(name, paramNames, signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, null) ?: parseDynamicArgs(parseBase, paramNames, null)
    }
}