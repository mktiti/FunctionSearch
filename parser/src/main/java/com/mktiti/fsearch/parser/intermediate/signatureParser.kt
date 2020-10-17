package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.TypeSignature
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.generated.SignatureParser.ClassTypeSignatureContext
import com.mktiti.fsearch.parser.generated.SignatureParser.TypeParametersContext

interface JavaSignatureParser {

    fun parseDefinedStaticType(param: ClassTypeSignatureContext): CompleteMinInfo.Static?

    fun parseDefinedDynamicType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo.Dynamic

    fun parseDefinedType(param: ClassTypeSignatureContext, typeArgs: List<String>): CompleteMinInfo<*>

    fun parseTypeParams(paramCtx: TypeParametersContext?, externalTypeParams: List<String>): List<TypeParameter>

    fun parseFunction(name: String, paramNames: List<String>?, signature: String): TypeSignature

    fun parseTemplateSignature(info: MinimalInfo, signature: String, externalTypeParams: List<TypeParameter>): TypeTemplate

    fun parseDirectTypeSignature(info: MinimalInfo, signature: String): DirectType?

}
