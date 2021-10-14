package com.mktiti.fsearch.parser.parse

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.parser.generated.SignatureParser.*
import com.mktiti.fsearch.util.indexOfOrNull
import org.antlr.v4.runtime.tree.TerminalNode

class DefaultSignatureInfoParser(
        private val infoRepo: JavaInfoRepo
) : JavaSignatureInfoParser {

    companion object {
        private fun IntStaticCmi.wrap() = TypeParamInfo.Sat(this)
        private fun DatInfo.wrap() = TypeParamInfo.Dat(this)
    }

    /** Package */
    private fun parsePackage(param: PackageSpecifierContext?): List<String> = if (param == null) {
        emptyList()
    } else {
        listOf(param.identifier().text) + parsePackage(param.packageSpecifier())
    }

    /** Array */
    private fun parseStaticArray(param: ArrayTypeSignatureContext): IntStaticCmi? {
        val type = parseStaticJavaType(param.javaTypeSignature()) ?: return null
        return IntStaticCmi(infoRepo.arrayType.toIntMinInfo(), listOf(type))
    }

    private fun parseDynamicArray(param: ArrayTypeSignatureContext, typeParams: List<String>, selfParamName: String?): DatInfo {
        val type = parseJavaType(param.javaTypeSignature(), typeParams, selfParamName)
        return DatInfo(
                template = infoRepo.arrayType.toIntMinInfo(),
                args = listOf(type)
        )
    }

    /** Primitive */
    private fun parsePrimitive(primitive: TerminalNode): IntStaticCmi {
        return infoRepo.primitive(PrimitiveType.fromSignature(primitive.text)).toIntMinInfo().complete()
    }

    /** Type params */
    private fun parseTypeParam(param: TypeParameterContext, typeParams: List<String>): TemplateTypeParamInfo {
        val selfName = param.identifier().text

        val interfaceBounds: List<TypeParamInfo> = param.interfaceBounds()?.interfaceBound()?.map {
            parseDefinedType(it.classTypeSignature(), typeParams, selfName)
        } ?: emptyList()

        val classBound: TypeParamInfo? = param.classBound().referenceTypeSignature()?.let {
            parseRefType(it, typeParams, selfName)
        }

        val bounds = if (classBound == null) interfaceBounds else (interfaceBounds + classBound)

        return TemplateTypeParamInfo(
                sign = selfName,
                bounds = bounds
        )
    }

    override fun parseTypeParams(paramCtx: TypeParametersContext?, externalTypeParams: List<String>): List<TemplateTypeParamInfo> {
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
    private fun parseStaticTypeArg(param: TypeArgumentContext): IntStaticCmi? {
        return if (param.WildcardIndicator() == null) {
            if (param.WILDCARD() != null) {
                MinimalInfo.anyWildcard.toIntMinInfo().complete()
            } else {
                parseStaticRefType(param.referenceTypeSignature())
            }
        } else {
            null
        }
    }

    private fun parseDynamicTypeArg(param: TypeArgumentContext, typeArgs: List<String>, selfParamName: String?): TypeParamInfo {
        return when (val bounded = param.WildcardIndicator()) {
            null -> {
                if (param.WILDCARD() != null) {
                    TypeParamInfo.Wildcard
                } else {
                    parseRefType(param.referenceTypeSignature(), typeArgs, selfParamName)
                }
            }
            else -> {
                val type = parseRefType(param.referenceTypeSignature(), typeArgs, selfParamName)
                if (bounded.symbol.text == "+") {
                    TypeParamInfo.BoundedWildcard.UpperWildcard(type)
                } else {
                    TypeParamInfo.BoundedWildcard.LowerWildcard(type)
                }
            }
        }
    }

    /** Defined type */
    private fun parseSimpleClassName(param: SimpleClassTypeSignatureContext): Pair<String, List<TypeArgumentContext>> {
        val args = param.typeArguments()?.typeArgument() ?: emptyList()
        return param.identifier().text.replace('$', '.') to args
    }

    private fun parseDefinedTypeBase(param: ClassTypeSignatureContext): Pair<IntMinInfo, List<TypeArgumentContext>> {
        val packageName = parsePackage(param.packageSpecifier())

        val postfixes = param.classTypeSignatureSuffix().map { it.simpleClassTypeSignature() }
        val (nameParts, argsList) = (listOf(param.simpleClassTypeSignature()) + postfixes).map {
            parseSimpleClassName(it)
        }.unzip()

        val fullName = nameParts.joinToString(separator = ".")
        val args = argsList.flatten()

        return IntMinInfo(packageName, fullName) to args
    }

    override fun parseDefinedStaticType(param: ClassTypeSignatureContext): IntStaticCmi? {
        val (info, args) = parseDefinedTypeBase(param)

        val fullTypeArgs = args.map { parseStaticTypeArg(it) }.liftNull() ?: return null

        return IntStaticCmi(
                base = info,
                args = fullTypeArgs
        )
    }

    override fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): DatInfo {
        val (info, args) = parseDefinedTypeBase(param)

        val fullTypeArgs = args.map { parseDynamicTypeArg(it, typeArgs, selfParamName) }

        return DatInfo(
                template = info,
                args = fullTypeArgs
        )
    }

    override fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): TypeParamInfo {
        return parseDefinedStaticType(param)?.wrap() ?: parseDefinedDynamicType(param, typeArgs, selfParamName).wrap()
    }

    /** Ref types */
    private fun parseStaticRefType(param: ReferenceTypeSignatureContext): IntStaticCmi? {
        return when {
            param.typeVariableSignature() != null -> null
            param.arrayTypeSignature() != null -> parseStaticArray(param.arrayTypeSignature())
            else -> parseDefinedStaticType(param.classTypeSignature())
        }
    }

    private fun parseDynamicRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>, selfParamName: String?): TypeParamInfo {
        return when {
            param.typeVariableSignature() != null -> {
                val ref = param.typeVariableSignature().identifier().text

                if (ref == selfParamName) {
                    TypeParamInfo.SelfRef
                } else {
                    val index = typeParams.indexOfOrNull(ref) ?: throw UndeclaredTypeArgReference(ref)
                    TypeParamInfo.Param(index)
                }
            }
            param.arrayTypeSignature() != null -> {
                parseDynamicArray(param.arrayTypeSignature(), typeParams, selfParamName).wrap()
            }
            else -> {
                parseDefinedDynamicType(param.classTypeSignature(), typeParams, selfParamName).wrap()
            }
        }
    }

    private fun parseRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>, selfParamName: String?): TypeParamInfo {
        return parseStaticRefType(param)?.wrap() ?: parseDynamicRefType(param, typeParams, selfParamName)
    }

    /** Java types */
    override fun parseStaticJavaType(param: JavaTypeSignatureContext): IntStaticCmi? {
        return when (val primitive = param.PrimitiveType()) {
            null -> parseStaticRefType(param.referenceTypeSignature())
            else -> parsePrimitive(primitive)
        }
    }

    override fun parseJavaType(param: JavaTypeSignatureContext, typeParams: List<String>, selfParamName: String?): TypeParamInfo {
        return parseStaticJavaType(param)?.wrap() ?: parseDynamicRefType(param.referenceTypeSignature(), typeParams, selfParamName)
    }

}