package com.mktiti.fsearch.parser.connect

import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.parser.intermediate.FunctionInfoResult
import com.mktiti.fsearch.parser.intermediate.TypeInfoResult

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
