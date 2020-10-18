package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class DefaultTypeParser(
        private val signatureParser: JavaSignatureParser
) : JavaSignatureTypeParser {

    constructor(infoRepo: JavaInfoRepo) : this(DefaultSignatureParser(infoRepo))

    private data class TypeSignatureParse(
            val signature: SignatureParser.ClassSignatureContext,
            val superSignatures: List<SignatureParser.ClassTypeSignatureContext>
    )

    private fun parseTypeSignatureBase(signature: String): TypeSignatureParse {
        val lexer = SignatureLexer(CharStreams.fromString(signature))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = SignatureParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val signatureCtx = parser.classSignature()

        val parentClass = signatureCtx.superclassSignature().classTypeSignature()
        val interfaces = signatureCtx.superinterfaceSignature().map { it.classTypeSignature() }

        return TypeSignatureParse(signatureCtx, listOf(parentClass) + interfaces)
    }

    override fun parseTemplateSignature(info: MinimalInfo, signature: String, externalTypeParams: List<TypeParameter>): TypeTemplate {
        val (signatureCtx, superContexts) = parseTypeSignatureBase(signature)

        val externalTpNames = externalTypeParams.map { it.sign }
        val typeParams = signatureParser.parseTypeParams(signatureCtx.typeParameters(), externalTpNames)
        val typeParamNames = externalTpNames + typeParams.map { it.sign }

        val supers = superContexts.map {
            signatureParser.parseDefinedType(it, typeParamNames).holder()
        }

        return TypeTemplate(
                info = info,
                typeParams = externalTypeParams + typeParams,
                superTypes = supers,
                virtual = false,
                samType = null
        )
    }

    override fun parseDirectTypeSignature(info: MinimalInfo, signature: String): Type.NonGenericType.DirectType? {
        val (signatureCtx, superContexts) = parseTypeSignatureBase(signature)

        if (signatureCtx.typeParameters()?.isEmpty == false) {
            return null
        }

        val supers = superContexts.map {
            signatureParser.parseDefinedStaticType(it) ?: return null
        }

        return Type.NonGenericType.DirectType(
                minInfo = info,
                superTypes = TypeHolder.staticIndirects(supers),
                samType = null,
                virtual = false
        )
    }

}