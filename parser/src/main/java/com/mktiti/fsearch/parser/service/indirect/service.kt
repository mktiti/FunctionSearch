package com.mktiti.fsearch.parser.service.indirect

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.MinimalInfo

interface JclTypeInfoCollector<in I> {

    data class Result(val javaRepo: JavaRepo, val jclResult: RawTypeInfoResult)

    fun collectJcl(info: I, name: String): Result

}

interface ArtifactTypeInfoCollector<in I> {

    fun collectRawInfo(info: I): RawTypeInfoResult

}

interface TypeInfoConnector {

    data class JclResult(val javaRepo: JavaRepo, val jclRepo: TypeRepo)

    fun connectJcl(
            rawInfoResults: RawTypeInfoResult
    ): JclResult

    fun connectArtifact(
            rawInfoResults: RawTypeInfoResult
    ): TypeResolver

}

interface TypeParamResolver {

    fun typeParams(info: MinimalInfo): List<TemplateTypeParamInfo>?

    operator fun get(info: MinimalInfo) = typeParams(info)

}

interface FunctionInfoCollector<in I> {

    data class FunctionInfoCollection(
            val staticFunctions: Collection<RawFunInfo<*>>,
            val instanceMethods: Map<MinimalInfo, Collection<RawFunInfo<*>>>
    )

    fun collectFunctions(info: I, infoRepo: JavaInfoRepo, typeParamResolver: TypeParamResolver): FunctionInfoCollection

}

interface FunctionConnector {

    data class FunctionCollection(
            val staticFunctions: Collection<FunctionObj>,
            val instanceMethods: Map<MinimalInfo, List<FunctionObj>>
    )

    fun connect(funInfo: FunctionInfoCollector.FunctionInfoCollection): FunctionCollection

}
