package com.mktiti.fsearch.parser.parse

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class DefaultTypeSignatureInfoBuilder(
        private val signatureParser: JavaSignatureInfoParser
) : JavaTypeSignatureInfoBuilder {

    constructor(infoRepo: JavaInfoRepo) : this(DefaultSignatureInfoParser(infoRepo))

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

    private sealed class SuperKindHelper<I>(val info: I) {
        class Dat(dat: DatInfo) : SuperKindHelper<DatInfo>(dat)
        class Sat(sat: IntStaticCmi) : SuperKindHelper<IntStaticCmi>(sat)
        class Direct(info: IntMinInfo) : SuperKindHelper<IntMinInfo>(info)
    }

    private inline fun <I, reified H : SuperKindHelper<I>> List<SuperKindHelper<*>>.superKind(): List<I>
        = filterIsInstance<H>().map { it.info }

    override fun parseTemplateSignature(
            info: IntMinInfo,
            signature: String,
            externalTypeParams: List<TemplateTypeParamInfo>,
            samTypeCreator: (typeParams: List<TemplateTypeParamInfo>) -> SamInfo.Generic?
    ): SemiInfo.TemplateInfo {
        val (signatureCtx, superContexts) = parseTypeSignatureBase(signature)

        val externalTpNames = externalTypeParams.map { it.sign }
        val selfTypeParams = signatureParser.parseTypeParams(signatureCtx.typeParameters(), externalTpNames)
        val typeParamNames = externalTpNames + selfTypeParams.map { it.sign }

        val supers = superContexts.map {
            val asStatic = signatureParser.parseDefinedStaticType(it)
            when {
                asStatic == null -> {
                    SuperKindHelper.Dat(signatureParser.parseDefinedDynamicType(it, typeParamNames, selfParamName = null))
                }
                asStatic.args.isEmpty() -> {
                    SuperKindHelper.Direct(asStatic.base)
                }
                else -> {
                    SuperKindHelper.Sat(asStatic)
                }
            }
        }

        val typeParams = externalTypeParams + selfTypeParams
        return SemiInfo.TemplateInfo(
                info = info,
                typeParams = typeParams,
                directSupers = supers.superKind<IntMinInfo, SuperKindHelper.Direct>(),
                satSupers = supers.superKind<IntStaticCmi, SuperKindHelper.Sat>(),
                datSupers = supers.superKind<DatInfo, SuperKindHelper.Dat>(),
                samType = samTypeCreator(typeParams)
        )
    }

    override fun parseDirectTypeSignature(info: IntMinInfo, signature: String, samInfo: SamInfo.Direct?): SemiInfo.DirectInfo? {
        val (signatureCtx, superContexts) = parseTypeSignatureBase(signature)

        if (signatureCtx.typeParameters()?.isEmpty == false) {
            return null
        }

        val (directSupers, satSupers) = superContexts.map {
            signatureParser.parseDefinedStaticType(it) ?: return null
        }.partition {
            it.args.isEmpty()
        }

        return SemiInfo.DirectInfo(
                info = info,
                directSupers = directSupers.map { it.base },
                satSupers = satSupers,
                samType = samInfo
        )
    }

}