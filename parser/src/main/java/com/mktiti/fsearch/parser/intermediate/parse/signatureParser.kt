package com.mktiti.fsearch.parser.intermediate.parse

import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParametersContext
import com.mktiti.fsearch.parser.intermediate.*

class UndeclaredTypeArgReference(typeArg: String) : Exception("Type argument $typeArg is undeclared!")

interface JavaSignatureInfoParser {

    fun parseDefinedStaticType(param: ClassTypeSignatureContext): CompleteMinInfo.Static?

    fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): DatInfo

    fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): TypeParamInfo

    fun parseTypeParams(paramCtx: TypeParametersContext?, externalTypeParams: List<String>): List<TemplateTypeParamInfo>

    fun parseStaticJavaType(param: SignatureParser.JavaTypeSignatureContext): CompleteMinInfo.Static?

    fun parseJavaType(param: SignatureParser.JavaTypeSignatureContext, typeParams: List<String>, selfParamName: String?): TypeParamInfo

}

interface JavaTypeSignatureInfoBuilder {

    fun parseTemplateSignature(
            info: MinimalInfo,
            signature: String,
            externalTypeParams: List<TemplateTypeParamInfo>,
            samTypeCreator: (typeParams: List<TemplateTypeParamInfo>) -> SamInfo.Generic?
    ): SemiInfo.TemplateInfo

    fun parseDirectTypeSignature(info: MinimalInfo, signature: String, samInfo: SamInfo.Direct?): SemiInfo.DirectInfo?

}

interface JavaFunctionSignatureInfoBuilder {

    fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TemplateTypeParamInfo>): FunSignatureInfo<*>

    fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo): FunSignatureInfo<*>

    fun parseDirectConstructor(type: MinimalInfo, signature: String, paramNames: List<String>?): FunSignatureInfo<*>

    fun parseTemplateConstructor(type: MinimalInfo, typeParams: List<TemplateTypeParamInfo>, signature: String, paramNames: List<String>?): FunSignatureInfo.Generic

    fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): FunSignatureInfo<*>

    fun parseDirectSam(paramNames: List<String>?, signature: String): FunSignatureInfo.Direct?

    fun parseGenericSam(paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TemplateTypeParamInfo>): FunSignatureInfo.Generic?

}
