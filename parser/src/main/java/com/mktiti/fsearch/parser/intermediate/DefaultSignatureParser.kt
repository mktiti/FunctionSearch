package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.generated.SignatureParser.ArrayTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.JavaTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.PackageSpecifierContext
import com.mktiti.fsearch.parser.generated.SignatureParser.ReferenceTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.SimpleClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeArgumentContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParameterContext
import com.mktiti.fsearch.util.indexOfOrNull
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

    private fun parseDynamicArray(param: ArrayTypeSignatureContext, typeParams: List<String>, selfParamName: String?): CompleteMinInfo.Dynamic {
        val type = parseJavaType(param.javaTypeSignature(), typeParams, selfParamName)
        return infoRepo.arrayType.dynamicComplete(listOf(type))
    }

    /** Primitive */
    private fun parsePrimitive(primitive: TerminalNode): CompleteMinInfo.Static {
        return infoRepo.primitive(PrimitiveType.fromSignature(primitive.text)).complete()
    }

    /** Type params */
    private fun parseTypeParam(param: TypeParameterContext, typeParams: List<String>): TypeParameter {
        val selfName = param.identifier().text

        val interfaceBounds: List<Substitution> = param.interfaceBounds()?.interfaceBound()?.map {
            TypeSubstitution(parseDefinedType(it.classTypeSignature(), typeParams, selfName).holder())
        } ?: emptyList()

        val classBound: Substitution? = param.classBound().referenceTypeSignature()?.let {
            parseRefType(it, typeParams, selfName)
        }

        val bounds = if (classBound == null) interfaceBounds else (interfaceBounds + classBound)

        return TypeParameter(
                sign = selfName,
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

    private fun parseDynamicTypeArg(param: TypeArgumentContext, typeArgs: List<String>, selfParamName: String?): ApplicationParameter {
        return when (val bounded = param.WildcardIndicator()) {
            null -> {
                if (param.WILDCARD() != null) {
                    TypeSubstitution.unboundedWildcard
                } else {
                    parseRefType(param.referenceTypeSignature(), typeArgs, selfParamName)
                }
            }
            else -> {
                val type = parseRefType(param.referenceTypeSignature(), typeArgs, selfParamName)
                val dir = if (bounded.symbol.text == "+") BoundDirection.UPPER else BoundDirection.LOWER
                BoundedWildcard.Dynamic(type, dir)
            }
        }
    }

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

    override fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): CompleteMinInfo.Dynamic {
        val (info, args) = parseDefinedTypeBase(param)

        val fullTypeArgs = args.map { parseDynamicTypeArg(it, typeArgs, selfParamName) }

        return CompleteMinInfo.Dynamic(
                base = info,
                args = fullTypeArgs
        )
    }

    override fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): CompleteMinInfo<*> {
        return parseDefinedStaticType(param) ?: parseDefinedDynamicType(param, typeArgs, selfParamName)
    }

    /** Ref types */
    private fun parseStaticRefType(param: ReferenceTypeSignatureContext): CompleteMinInfo.Static? {
        return when {
            param.typeVariableSignature() != null -> null
            param.arrayTypeSignature() != null -> parseStaticArray(param.arrayTypeSignature())
            else -> parseDefinedStaticType(param.classTypeSignature())
        }
    }

    private fun parseDynamicRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>, selfParamName: String?): Substitution {
        return when {
            param.typeVariableSignature() != null -> {
                val ref = param.typeVariableSignature().identifier().text

                if (ref == selfParamName) {
                    Substitution.SelfSubstitution
                } else {
                    val index = typeParams.indexOfOrNull(ref) ?: throw UndeclaredTypeArgReference(ref)
                    ParamSubstitution(index)
                }
            }
            param.arrayTypeSignature() != null -> {
                TypeSubstitution(parseDynamicArray(param.arrayTypeSignature(), typeParams, selfParamName).holder())
            }
            else -> {
                TypeSubstitution(parseDefinedDynamicType(param.classTypeSignature(), typeParams, selfParamName).holder())
            }
        }
    }

    private fun parseRefType(param: ReferenceTypeSignatureContext, typeParams: List<String>, selfParamName: String?): Substitution {
        return parseStaticRefType(param).wrap() ?: parseDynamicRefType(param, typeParams, selfParamName)
    }

    /** Java types */
    override fun parseStaticJavaType(param: JavaTypeSignatureContext): CompleteMinInfo.Static? {
        return when (val primitive = param.PrimitiveType()) {
            null -> parseStaticRefType(param.referenceTypeSignature())
            else -> parsePrimitive(primitive)
        }
    }

    override fun parseJavaType(param: JavaTypeSignatureContext, typeParams: List<String>, selfParamName: String?): Substitution {
        return parseStaticJavaType(param).wrap() ?: parseDynamicRefType(param.referenceTypeSignature(), typeParams, selfParamName)
    }

}