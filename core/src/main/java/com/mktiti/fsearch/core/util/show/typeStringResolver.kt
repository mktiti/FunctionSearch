package com.mktiti.fsearch.core.util.show

import com.mktiti.fsearch.core.fit.FittingMap
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.SemiType

interface TypeStringResolver {

    fun resolveName(info: CompleteMinInfo.Static): String

    fun resolveName(info: CompleteMinInfo.Dynamic, typeParams: List<String> = emptyList()): String

    fun resolveSemiName(semi: SemiType): String

    fun resolveName(info: CompleteMinInfo<*>, typeParams: List<String> = emptyList()) = when (info) {
        is CompleteMinInfo.Static -> resolveName(info)
        is CompleteMinInfo.Dynamic -> resolveName(info, typeParams)
    }

    fun resolveFittingMap(result: FittingMap): String

    fun resolveQuery(query: QueryType): String

    fun resolveFun(function: FunctionObj): String

}