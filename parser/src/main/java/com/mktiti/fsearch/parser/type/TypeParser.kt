package com.mktiti.fsearch.parser.type

/*
class TypeParser(
        private val infoRepo: JavaInfoRepo
) {

    sealed class ParsedType(
            val supersTypes: List<ImParam.Type>
    ) {

        class Direct(
                supersTypes: List<ImParam.Type>
        ) : ParsedType(supersTypes)

        class Template(
                val typeParams: List<TypeParamInfo>,
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

        val typeParams = FunctionParser.parseTypeParams(signatureCtx.typeParameters())

        val parentClass = signatureCtx.superclassSignature().classTypeSignature()
        val interfaces = signatureCtx.superinterfaceSignature().map { it.classTypeSignature() }
        val supers = (interfaces + parentClass).map { FunctionParser.parseDefinedType(it) }

        return if (typeParams.isEmpty()) {
            ParsedType.Direct(supers)
        } else {
            val typeParamInfos = typeParams.map { param ->
                TypeParamInfo(
                        sign = param.sign,
                        bounds = param.bounds.map {
                            FunctionParser.
                        }
                )
            }

            ParsedType.Template(typeParams, supers)
        }
    }

}
 */