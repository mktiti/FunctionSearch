package function

import ExceptionErrorListener
import PrimitiveType
import SignatureLexer
import SignatureParser
import SignatureParser.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import type.MinimalInfo

private fun parsePackage(param: PackageSpecifierContext?): List<String> = if (param == null) {
    emptyList()
} else {
    listOf(param.identifier().text) + parsePackage(param.packageSpecifier())
}

private fun parseTypeArgs(param: TypeArgumentsContext?): List<ImParam> = when (param) {
    null -> emptyList()
    else -> param.typeArgument().map(::parseTypeArg)
}

private fun parseTypeArg(param: TypeArgumentContext): ImParam {
    return when (val bounded = param.WildcardIndicator()) {
        null -> {
            if (param.WILDCARD() != null) {
                ImParam.Wildcard
            } else {
                parseRefType(param.referenceTypeSignature())
            }
        }
        else -> {
            val type = parseRefType(param.referenceTypeSignature())
            if (bounded.symbol.text == "+") {
                ImParam.UpperWildcard(type)
            } else {
                ImParam.LowerWildcard(type)
            }
        }
    }
}

private fun parseSimpleClass(param: SimpleClassTypeSignatureContext): Pair<String, List<ImParam>> {
    return param.identifier().text to parseTypeArgs(param.typeArguments())
}

fun parseDefinedType(param: ClassTypeSignatureContext): ImParam.Type {
    val (name, typeArgs) = parseSimpleClass(param.simpleClassTypeSignature())
    val postfixes = param.classTypeSignatureSuffix().map { parseSimpleClass(it.simpleClassTypeSignature()) }

    val fullTypeArgsAttributes =  typeArgs + postfixes.flatMap { it.second }

    val prePackage = parsePackage(param.packageSpecifier())
    val (packageName, finalName) = if (name.contains("$")) {
        val split = name.split("$")
        (prePackage + split.dropLast(1)) to split.last()
    } else {
        prePackage to name
    }

    return ImParam.Type(
            info = MinimalInfo(packageName, finalName),
            typeArgs = fullTypeArgsAttributes
    )
}

private fun parseJavaType(param: JavaTypeSignatureContext): ImParam {
    return when (val primitive = param.PrimitiveType()) {
        null -> parseRefType(param.referenceTypeSignature())
        else -> ImParam.Primitive(PrimitiveType.fromSignature(primitive.text))
    }
}

private fun parseRefType(param: ReferenceTypeSignatureContext): ImParam {
    return when {
        param.typeVariableSignature() != null -> ImParam.TypeParamRef(param.typeVariableSignature().identifier().text)
        param.arrayTypeSignature() != null -> ImParam.Array(parseJavaType(param.arrayTypeSignature().javaTypeSignature()))
        else -> parseDefinedType(param.classTypeSignature())
    }
}

private fun parseTypeParam(param: TypeParameterContext): ImTypeParam {
    val classBound = param.classBound().referenceTypeSignature()?.let { listOf(parseRefType(it)) } ?: emptyList()
    val interfaceBounds = param.interfaceBounds()?.interfaceBound()?.map { parseDefinedType(it.classTypeSignature()) } ?: emptyList()

    return ImTypeParam(
        sign = param.identifier().text,
        bounds = classBound + interfaceBounds
    )
}

fun parseTypeParams(params: TypeParametersContext?): List<ImTypeParam> {
    return when (params) {
        null -> emptyList()
        else -> params.typeParameter().map(::parseTypeParam)
    }
}

private fun parseReturnType(value: ReturnTypeContext): ImParam {
    return when (val type = value.javaTypeSignature()) {
        null -> ImParam.Void
        else -> parseJavaType(type)
    }
}

fun parseFunction(name: String, signature: String): ImSignature {
    val lexer = SignatureLexer(CharStreams.fromString(signature))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ExceptionErrorListener)

    val parser = SignatureParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(ExceptionErrorListener)

    val signatureCtx = parser.methodSignature()

    return ImSignature(
        name = name,
        typeParams = parseTypeParams(signatureCtx.typeParameters()),
        inputs = signatureCtx.javaTypeSignature().map(::parseJavaType),
        output = parseReturnType(signatureCtx.returnType())
    )
}