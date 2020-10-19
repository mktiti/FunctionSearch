package com.mktiti.fsearch.core.util.show

import com.mktiti.fsearch.core.fit.FittingMap
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeTemplate

interface TypePrint {

    fun printTypeTemplate(template: TypeTemplate)

    fun printType(type: Type<*>)

    fun printSemiType(type: SemiType)

    fun printFittingMap(result: FittingMap)

    fun print(query: QueryType)

    fun printFun(function: FunctionObj)

    fun printFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType)

    fun printOrderedFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType)

}