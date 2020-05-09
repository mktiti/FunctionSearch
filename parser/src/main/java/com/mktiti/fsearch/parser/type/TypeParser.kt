package com.mktiti.fsearch.parser.type

import SignatureLexer
import SignatureParser
import com.mktiti.fsearch.parser.function.ImParam
import com.mktiti.fsearch.parser.function.ImTypeParam
import com.mktiti.fsearch.parser.function.parseDefinedType
import com.mktiti.fsearch.parser.function.parseTypeParams
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

sealed class ParsedType(
        val supersTypes: List<ImParam.Type>
) {

    class Direct(
            supersTypes: List<ImParam.Type>
    ) : ParsedType(supersTypes)

    class Template(
            val typeParams: List<ImTypeParam>,
            superTypes: List<ImParam.Type>
    ) : ParsedType(superTypes)

}

fun parseType(signature: String, nestTypeParams: List<ImTypeParam>): ParsedType {
    val lexer = SignatureLexer(CharStreams.fromString(signature))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ExceptionErrorListener)

    val parser = SignatureParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(ExceptionErrorListener)

    val signatureCtx = parser.classSignature()

    val typeParams = nestTypeParams + parseTypeParams(signatureCtx.typeParameters())

    val parentClass = signatureCtx.superclassSignature().classTypeSignature()
    val interfaces = signatureCtx.superinterfaceSignature().map { it.classTypeSignature() }
    val supers = (interfaces + parentClass).map { parseDefinedType(it) }

    return if (typeParams.isEmpty()) {
        ParsedType.Direct(supers)
    } else {
        ParsedType.Template(typeParams, supers)
    }
}