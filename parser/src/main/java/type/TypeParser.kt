package type

import ExceptionErrorListener
import SignatureLexer
import SignatureParser
import function.ImParam
import function.ImTypeParam
import function.parseDefinedType
import function.parseTypeParams
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

fun parseType(signature: String): ParsedType {
    val lexer = SignatureLexer(CharStreams.fromString(signature))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ExceptionErrorListener)

    val parser = SignatureParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(ExceptionErrorListener)

    val signatureCtx = parser.classSignature()

    val typeParams = parseTypeParams(signatureCtx.typeParameters())

    val parentClass = signatureCtx.superclassSignature().classTypeSignature()
    val interfaces = signatureCtx.superinterfaceSignature().map { it.classTypeSignature() }
    val supers = (interfaces + parentClass)
            .map { parseDefinedType(it) }

    return if (typeParams.isEmpty()) {
        ParsedType.Direct(supers)
    } else {
        ParsedType.Template(typeParams, supers)
    }
}