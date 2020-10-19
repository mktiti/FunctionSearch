package com.mktiti.fsearch.core.util.show

import com.mktiti.fsearch.core.fit.FittingMap
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.util.SemiVisitor
import java.io.PrintStream

class JavaTypePrinter(
        private val infoRepo: JavaInfoRepo,
        typeResolver: TypeResolver,
        private val stringResolver: TypeStringResolver = JavaTypeStringResolver(infoRepo),
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

    override fun printType(type: Type<*>) {
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
            output.println(stringResolver.resolveSemiName(node))

            if (hasMore) {
                siblingDepths += depth
            } else {
                siblingDepths -= depth
            }
        }

        output.println("==================")
    }

    override fun print(query: QueryType) {
        output.println(stringResolver.resolveQuery(query))
    }

    override fun printFun(function: FunctionObj) {
        output.println(stringResolver.resolveFun(function))
    }

    override fun printFittingMap(result: FittingMap) {
        output.print("Query fit, where ")
        output.println(stringResolver.resolveFittingMap(result))
    }

    private fun printFitBase(function: FunctionObj, query: QueryType, strategy: (QueryType, FunctionObj) -> FittingMap?) {
        output.println("==================")
        output.println("Function: ${stringResolver.resolveFun(function)}")
        output.println("Query: ${stringResolver.resolveQuery(query)}")
        when (val fitResult = strategy(query, function)) {
            null -> output.println("Failed to match function with query")
            else -> printFittingMap(fitResult)
        }
        output.println("==================")
    }

    override fun printFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType) {
        printFitBase(function, query, queryFitter::fitsQuery)
    }

    override fun printOrderedFit(queryFitter: QueryFitter, function: FunctionObj, query: QueryType) {
        printFitBase(function, query, queryFitter::fitsOrderedQuery)
    }

}
