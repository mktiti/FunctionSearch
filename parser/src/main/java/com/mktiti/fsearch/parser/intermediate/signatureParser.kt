package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.parser.function.FunctionBuilder
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParametersContext
import com.mktiti.fsearch.parser.type.CreatorInfo

interface JavaSignatureParser {

    fun parseDefinedStaticType(param: ClassTypeSignatureContext): CompleteMinInfo.Static?

    fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo.Dynamic

    fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo<*>

    fun parseTypeParams(paramCtx: TypeParametersContext?, externalTypeParams: List<String>): List<TypeParameter>

    fun parseStaticJavaType(param: SignatureParser.JavaTypeSignatureContext): CompleteMinInfo.Static?

    fun parseJavaType(param: SignatureParser.JavaTypeSignatureContext, typeParams: List<String>): Substitution

}

interface JavaSignatureTypeParser {

    fun parseTemplateSignature(info: MinimalInfo, signature: String, externalTypeParams: List<TypeParameter>): TypeTemplate

    fun parseDirectTypeSignature(info: MinimalInfo, signature: String): DirectType?

}

interface JavaSignatureFunctionParser {

    fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: TypeTemplate): TypeSignature

    fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: CompleteMinInfo.Static): TypeSignature

    fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature

}
