package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.type.TypeTemplate
import java.io.PrintStream

class JavaTypePrinter(
        typeResolver: TypeResolver,
        private val output: PrintStream = System.out
) : TypePrint {

    private object TypePrintConst {
        const val pre = "  ├──"
        const val emptyPre = "     "
        const val preLast = "  └──"
        const val preSibling = "  │  "
    }

    private val semiVisitor = SemiVisitor(typeResolver)

    override fun printTypeTemplate(template: TypeTemplate) {
        output.println("${template.info} Type Template")
        printSemiType(template)
    }

    override fun printType(type: Type) {
        val info = when (type) {
            is DirectType -> "Direct Type"
            is StaticAppliedType -> "Statically Applied Type"
            is DynamicAppliedType -> "Dynamically Applied Type"
        }
        output.println("$info ${type.info}")
        printSemiType(type)
    }

    override fun printSemiType(type: SemiType) {
        val siblingDepths: MutableSet<Int> = sortedSetOf()

        semiVisitor.visitSupersDf(type) { node, depth, hasMore ->
            for (d in (0 until depth - 1)) {
                if ((d + 1) in siblingDepths) {
                    output.print(TypePrintConst.preSibling)
                } else {
                    output.print(TypePrintConst.emptyPre)
                }
            }

            if (depth > 0) {
                output.print(if (hasMore) TypePrintConst.pre else TypePrintConst.preLast)
            }
            output.println(node.fullName)

            if (hasMore) {
                siblingDepths += depth
            } else {
                siblingDepths -= depth
            }
        }

        output.println("==================")
    }

    override fun printFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType) {
        output.println("==================")
        output.println("Function: $function")
        output.println("Query: $query")
        val result = when (val fitResult = queryFitter.fitsQuery(query, function)) {
            null -> "Failed to match function with query"
            else -> fitResult.toString()
        }
        output.println(result)
        output.println("==================")
    }

}
