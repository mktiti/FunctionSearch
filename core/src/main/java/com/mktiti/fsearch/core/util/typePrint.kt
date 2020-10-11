package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.type.TypeTemplate

private object TypePrintConst {
    const val pre = "  ├──"
    const val emptyPre = "     "
    const val preLast = "  └──"
    const val preSibling = "  │  "
}

fun printTypeTemplate(template: TypeTemplate) {
    println("${template.info} Type Template")
    printSemiType(template)
}

fun printType(type: Type) {
    val info = when (type) {
        is DirectType -> "Direct Type"
        is StaticAppliedType -> "Statically Applied Type"
        is DynamicAppliedType -> "Dynamically Applied Type"
    }
    println("$info ${type.info}")
    printSemiType(type)
}

fun printSemiType(type: SemiType) {
    val siblingDepths: MutableSet<Int> = sortedSetOf()
    /*
    type.supersTree.walkDf { node, depth, hasMore ->
        for (d in (0 until depth - 1)) {
            if ((d + 1) in siblingDepths) {
                print(TypePrintConst.preSibling)
            } else {
                print(TypePrintConst.emptyPre)
            }
        }

        if (depth > 0) {
            print(if (hasMore) TypePrintConst.pre else TypePrintConst.preLast)
        }
        println(node.value.fullName)

        if (hasMore) {
            siblingDepths += depth
        } else {
            siblingDepths -= depth
        }
    }
     */
    println("==================")
}

fun printFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType) {
    println("==================")
    println("Function: $function")
    println("Query: $query")
    val result = when (val fitResult = queryFitter.fitsQuery(query, function)) {
        null -> "Failed to match function with query"
        else -> fitResult.toString()
    }
    println(result)
    println("==================")
}
