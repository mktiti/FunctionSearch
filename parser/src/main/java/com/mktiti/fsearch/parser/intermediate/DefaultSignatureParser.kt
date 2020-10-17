package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.generated.SignatureLexer
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.generated.SignatureParser.ArrayTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.JavaTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.PackageSpecifierContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ReferenceTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ReturnTypeContext
import com.mktiti.fsearch.parser.generated.SignatureParser.SimpleClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeArgumentContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParameterContext
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import com.mktiti.fsearch.util.indexOfOrNull
import com.mktiti.fsearch.util.repeat
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode

class DefaultSignatureParser(
        private val infoRepo: JavaInfoRepo
) : JavaSignatureParser {

    companion object {
        private fun CompleteMinInfo.Static?.wrap(): StaticTypeSubstitution? = this?.let { TypeSubstitution(it.holder()) }
    }

    /** Package */
    private fun parsePackage(param: PackageSpecifierContext?): List<String> = if (param == null) {
        emptyList()
    } else {
        listOf(param.identifier().text) + parsePackage(param.packageSpecifier())
    }

    /** Array */
    private fun parseStaticArray(param: ArrayTypeSignatureContext): CompleteMinInfo.Static? {
        val type = parseStaticJavaType(param.javaTypeSignature()) ?: return null
        return infoRepo.arrayType.staticComplete(listOf(type))
    }

    private fun parseDynamicArray(param: ArrayTypeSignatureContext, typeParams: List<String>): CompleteMinInfo.Dynamic {
        val type = parseJavaType(param.javaTypeSignature(), typeParams)
        return infoRepo.arrayType.dynamicComplete(listOf(type))
    }

    /*
    private fun parseArray(param: ArrayTypeSignatureContext, typeParams: List<String>): CompleteMinInfo<*> {
        return parseStaticArray(param) ?: parseDynamicArray(param, typeParams)
    }
     */

    /** Primitive */
    private fun parsePrimitive(primitive: TerminalNode): CompleteMinInfo.Static {
        return infoRepo.primitive(PrimitiveType.fromSignature(primitive.text)).complete()
    }

    /** Type params */
    private fun parseTypeParam(param: TypeParameterContext, typeParams: List<String>): TypeParameter {
        val interfaceBounds: List<Substitution> = param.interfaceBounds()?.interfaceBound()?.map {
            TypeSubstitution(parseDefinedType(it.classTypeSignature(), typeParams).holder())
        } ?: emptyList()

        val classBound: Substitution? = param.classBound().referenceTypeSignature()?.let {
            parseRefType(it, typeParams)
        }

        val bounds = if (classBound == null) interfaceBounds else (interfaceBounds + classBound)

        return TypeParameter(
                sign = param.identifier().text,
                bounds = TypeBounds(bounds.toSet())
        )
    }

    override fun parseTypeParams(paramCtx: SignatureParser.TypeParametersContext?, externalTypeParams: List<String>): List<TypeParameter> {
        return when (paramCtx) {
            null -> emptyList()
            else -> {
                val params = paramCtx.typeParameter()
                val paramNames = externalTypeParams + params.map { it.identifier().text }
                params.map {
                    parseTypeParam(it, paramNames)
                }
            }
        }
    }

    /** Type args */
    private fun parseStaticTypeArg(param: TypeArgumentContext): CompleteMinInfo.Static? {
        return if (param.WildcardIndicator() == null) {
            if (param.WILDCARD() != null) {
                MinimalInfo.anyWildcard.complete()
            } else {
                parseStaticRefType(param.referenceTypeSignature())
            }
        } else {
            null
        }
    }

    private fun parseDynamicTypeArg(param: TypeArgumentContext, typeArgs: List<String>): ApplicationParameter {
        return when (val bounded = param.WildcardIndicator()) {
            null -> {
                if (param.WILDCARD() != null) {
                    TypeSubstitution.unboundedWildcard
                } else {
                    parseRefType(param.referenceTypeSignature(), typeArgs)
                }
            }
            else -> {
                val type = parseRefType(param.referenceTypeSignature(), typeArgs)
                val dir = if (bounded.symbol.text == "+") BoundDirection.UPPER else BoundDirection.LOWER
                BoundedWildcard.Dynamic(type, dir)
            }
        }
    }

    /*
    private fun <T> parseTypeArgsBase(param: TypeArgumentsContext?): List<T> = when (param) {
        null -> emptyList()
        else -> {
            param.typeArgument().map(::parseTypeArg)
        }
    }
     */

    /** Defined type */
    private fun parseSimpleClassName(param: SimpleClassTypeSignatureContext): Pair<String, List<TypeArgumentContext>> {
        val args = param.typeArguments()?.typeArgument() ?: emptyList()
        return param.identifier().text.replace('$', '.') to args
    }

    private fun parseDefinedTypeBase(param: ClassTypeSignatureContext): Pair<MinimalInfo, List<TypeArgumentContext>> {
        val packageName = parsePackage(param.packageSpecifier())

        val postfixes = param.classTypeSignatureSuffix().map { it.simpleClassTypeSignature() }
        val (nameParts, argsList) = (listOf(param.simpleClassTypeSignature()) + postfixes).map {
            parseSimpleClassName(it)
        }.unzip()

        val fullName = nameParts.joinToString(separator = ".")
        val args = argsList.flatten()

        return MinimalInfo(packageName, fullName) to args
    }

    override fun parseDefinedStaticType(param: ClassTypeSignatureContext): CompleteMinInfo.Static? {
        val (info, args) = parseDefinedTypeBase(param)

        val fullTypeArgs = args.map { parseStaticTypeArg(it) }.liftNull() ?: return null

        return CompleteMinInfo.Static(
                base = info,
                args = fullTypeArgs
        )
    }

    override fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo.Dynamic {
        val (info, args) = parseDefinedTypeBase(param)

        val fullTypeArgs = args.map { parseDynamicTypeArg(it, typeArgs) }

        return CompleteMinInfo.Dynamic(
                base = info,
                args = fullTypeArgs
        )
    }

    override fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo<*> {
        return parseDefinedStaticType(param) ?: parseDefinedDynamicType(param, typeArgs)
    }

    /** Ref types */
    private fun parseStaticRefType(param: ReferenceTypeSignatureContext): CompleteMinInfo.Static? {
        return when {
            param.typeVariableSignature() != null -> null
            param.arrayTypeSignature() != null -> parseStaticArray(param.arrayTypeSignature())
            else -> parseDefinedStaticType(param.classTypeSignature())
        }
    }

    private fun parseDynamicRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>): Substitution {
        return when {
            param.typeVariableSignature() != null -> {
                val ref = param.typeVariableSignature().identifier().text
                val index = typeParams.indexOfOrNull(ref) ?: error("Undeclared type arg $ref!")
                ParamSubstitution(index)
            }
            param.arrayTypeSignature() != null -> {
                TypeSubstitution(parseDynamicArray(param.arrayTypeSignature(), typeParams).holder())
            }
            else -> {
                TypeSubstitution(parseDefinedDynamicType(param.classTypeSignature(), typeParams).holder())
            }
        }
    }

    private fun parseRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>): Substitution {
        return parseStaticRefType(param).wrap() ?: parseDynamicRefType(param, typeParams)
    }

    /** Java types */
    private fun parseStaticJavaType(param: JavaTypeSignatureContext): CompleteMinInfo.Static? {
        return when (val primitive = param.PrimitiveType()) {
            null -> parseStaticRefType(param.referenceTypeSignature())
            else -> parsePrimitive(primitive)
        }
    }

    fun parseJavaType(param: JavaTypeSignatureContext, typeParams: List<String>): Substitution {
        return parseStaticJavaType(param).wrap() ?: parseDynamicRefType(param.referenceTypeSignature(), typeParams)
    }

    /** Function */
    private fun parseStaticReturnType(value: ReturnTypeContext): StaticTypeSubstitution? = when (val type = value.javaTypeSignature()) {
        null -> infoRepo.voidType.complete()
        else -> parseStaticJavaType(type)
    }.wrap()

    private fun parseReturnType(value: ReturnTypeContext, typeParams: List<String>): Substitution {
        return parseStaticReturnType(value) ?: parseJavaType(value.javaTypeSignature(), typeParams)
    }

    override fun parseFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature {
        val lexer = SignatureLexer(CharStreams.fromString(signature))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = SignatureParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val signatureCtx = parser.methodSignature()
        val typeParams = parseTypeParams(signatureCtx.typeParameters(), emptyList())

        fun <T> List<T>.withNames(): List<Pair<String, T>> = if (paramNames == null) {
            mapIndexed { i, param -> "\$arg$i" to param }
        } else {
            val missingCount = maxOf(0, signatureCtx.javaTypeSignature().size - paramNames.size)
            val initNames: List<String?> = paramNames + null.repeat(missingCount)
            zip(initNames).mapIndexed { i, (param, name) ->
                (name ?: "\$arg$i") to param
            }
        }

        return if (typeParams.isEmpty()) {
            val inputs = signatureCtx.javaTypeSignature().withNames().map { (name, param) ->
                name to parseStaticJavaType(param).wrap()!!
            }

            TypeSignature.DirectSignature(
                    inputParameters = inputs,
                    output = parseStaticReturnType(signatureCtx.returnType())!!
            )
        } else {
            val typeParamNames = typeParams.map { it.sign }

            val inputs = signatureCtx.javaTypeSignature().withNames().map { (name, param) ->
                name to parseJavaType(param, typeParamNames)
            }

            TypeSignature.GenericSignature(
                    typeParameters = typeParams,
                    inputParameters = inputs,
                    output = parseReturnType(signatureCtx.returnType(), typeParamNames)
            )
        }
    }

    private data class TypeSignatureParse(
            val signature: ClassSignatureContext,
            val superSignatures: List<ClassTypeSignatureContext>
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
        val typeParams = parseTypeParams(signatureCtx.typeParameters(), externalTpNames)
        val typeParamNames = externalTpNames + typeParams.map { it.sign }

        val supers = superContexts.map {
            parseDefinedType(it, typeParamNames).holder()
        }

        return TypeTemplate(
                info = info,
                typeParams = externalTypeParams + typeParams,
                superTypes = supers,
                virtual = false,
                samType = null
        )
    }

    override fun parseDirectTypeSignature(info: MinimalInfo, signature: String): DirectType? {
        val (signatureCtx, superContexts) = parseTypeSignatureBase(signature)

        if (signatureCtx.typeParameters()?.isEmpty == false) {
            return null
        }

        val supers = superContexts.map {
            parseDefinedStaticType(it) ?: return null
        }

        return DirectType(
                minInfo = info,
                superTypes = TypeHolder.staticIndirects(supers),
                samType = null,
                virtual = false
        )
    }

}