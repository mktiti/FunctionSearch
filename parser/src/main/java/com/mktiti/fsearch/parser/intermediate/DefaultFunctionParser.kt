package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
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
        return parseStaticReturnType(value) ?: signatureParser.parseJavaType(value.javaTypeSignature(), typeParams, selfParamName = null)
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
            name to signatureParser.parseJavaType(param, typeParamNames, selfParamName = null)
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

    private fun parseFunctionBase(signature: String, typeLevelTypeParams: List<TypeParameter>): ParseBase {
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

    override fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TypeParameter>): TypeSignature {
        val parseBase = parseFunctionBase(signature, implicitThisTps)
        val thisParam = implicitThis.dynamicComplete(implicitThisTps.mapIndexed { i, _ -> Substitution.ParamSubstitution(i) })
        return parseDynamicArgs(parseBase, paramNames, TypeSubstitution(thisParam.holder()))
    }

    override fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: TypeTemplate): TypeSignature {
        return parseTemplateFunction(name, paramNames, signature, implicitThis.info, implicitThis.typeParams)
    }

    override fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: CompleteMinInfo.Static): TypeSignature {
        val parseBase = parseFunctionBase(signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, implicitThis) ?: parseDynamicArgs(parseBase, paramNames, TypeSubstitution(implicitThis.holder()))
    }

    override fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature {
        val parseBase = parseFunctionBase(signature, emptyList())
        return parseStaticArgs(parseBase, paramNames, null) ?: parseDynamicArgs(parseBase, paramNames, null)
    }

    override fun parseDirectSam(paramNames: List<String>?, signature: String): SamType.DirectSam? {
        val parseBase = parseFunctionBase(signature, emptyList())

        return if (parseBase.typeParams.isEmpty()) {
            parseStaticArgs(parseBase, paramNames, null)?.run {
                SamType.DirectSam(
                        explicit = false,
                        inputs = inputParameters.map { it.second.holder.info.holder() }, // TODO ...
                        output = output.holder.info.holder()
                )
            }
        } else {
            null
        }
    }

    override fun parseGenericSam(paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TypeParameter>): SamType.GenericSam? {
        val parseBase = parseFunctionBase(signature, implicitThisTps)
        val parsed = parseDynamicArgs(parseBase, paramNames, null)

        return if (parsed.typeParameters.size == implicitThisTps.size) {
            SamType.GenericSam(
                    explicit = false,
                    inputs = parsed.inputParameters.map { it.second },
                    output = parsed.output
            )
        } else {
            null
        }
    }
}
