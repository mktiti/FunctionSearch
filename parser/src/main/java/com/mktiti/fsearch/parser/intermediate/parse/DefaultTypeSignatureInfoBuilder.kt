package com.mktiti.fsearch.parser.intermediate.parse

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.intermediate.DatInfo
import com.mktiti.fsearch.parser.intermediate.SamInfo
import com.mktiti.fsearch.parser.intermediate.SemiInfo
import com.mktiti.fsearch.parser.intermediate.TemplateTypeParamInfo
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
        class Sat(sat: CompleteMinInfo.Static) : SuperKindHelper<CompleteMinInfo.Static>(sat)
        class Direct(info: MinimalInfo) : SuperKindHelper<MinimalInfo>(info)
    }

    private inline fun <I, reified H : SuperKindHelper<I>> List<SuperKindHelper<*>>.superKind(): List<I>
        = filterIsInstance<H>().map { it.info }

    override fun parseTemplateSignature(
            info: MinimalInfo,
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
                directSupers = supers.superKind<MinimalInfo, SuperKindHelper.Direct>(),
                satSupers = supers.superKind<CompleteMinInfo.Static, SuperKindHelper.Sat>(),
                datSupers = supers.superKind<DatInfo, SuperKindHelper.Dat>(),
                samInfo = samTypeCreator(typeParams)
        )
    }

    override fun parseDirectTypeSignature(info: MinimalInfo, signature: String, samInfo: SamInfo.Direct?): SemiInfo.DirectInfo? {
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
                samInfo = samInfo
        )
    }

}