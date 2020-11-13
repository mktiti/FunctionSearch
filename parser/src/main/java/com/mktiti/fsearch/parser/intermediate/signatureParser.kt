package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.generated.SignatureParser
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParametersContext

interface JavaSignatureParser {

    fun parseDefinedStaticType(param: ClassTypeSignatureContext): CompleteMinInfo.Static?

    fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): CompleteMinInfo.Dynamic

    fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>, selfParamName: String?): CompleteMinInfo<*>

    fun parseTypeParams(paramCtx: TypeParametersContext?, externalTypeParams: List<String>): List<TypeParameter>

    fun parseStaticJavaType(param: SignatureParser.JavaTypeSignatureContext): CompleteMinInfo.Static?

    fun parseJavaType(param: SignatureParser.JavaTypeSignatureContext, typeParams: List<String>, selfParamName: String?): Substitution

}

interface JavaSignatureTypeParser {

    fun parseTemplateSignature(
            info: MinimalInfo,
            signature: String,
            externalTypeParams: List<TypeParameter>,
            samTypeCreator: (typeParams: List<TypeParameter>) -> SamType.GenericSam?
    ): TypeTemplate

    fun parseDirectTypeSignature(info: MinimalInfo, signature: String, samType: SamType.DirectSam?): DirectType?

}

interface JavaSignatureFunctionParser {

    fun parseDirectSam(paramNames: List<String>?, signature: String): SamType.DirectSam?

    fun parseGenericSam(paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TypeParameter>): SamType.GenericSam?

    fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: MinimalInfo, implicitThisTps: List<TypeParameter>): TypeSignature

    fun parseTemplateFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: TypeTemplate): TypeSignature

    fun parseDirectFunction(name: String, paramNames: List<String>?, signature: String, implicitThis: CompleteMinInfo.Static): TypeSignature

    fun parseStaticFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature

}
