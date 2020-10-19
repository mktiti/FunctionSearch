package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.parser.function.FunctionParser
import com.mktiti.fsearch.parser.function.ImParam
import com.mktiti.fsearch.parser.function.ImTypeParam
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

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