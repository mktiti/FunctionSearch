package com.mktiti.fsearch.model.build.service

import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.model.build.intermediate.FunctionInfoResult
import com.mktiti.fsearch.model.build.intermediate.TypeInfoResult

interface TypeInfoConnector {

    fun connectJcl(
            infoResults: TypeInfoResult
    ): JclTypeResult

    fun connectArtifact(
            infoResults: TypeInfoResult
    ): TypeResolver

}

interface FunctionConnector {

    fun connect(funInfo: FunctionInfoResult): FunctionCollection

}
