import Type.DynamicAppliedType
import Type.NonGenericType.DirectType
import Type.NonGenericType.StaticAppliedType

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
    println("==================")
}

fun printFit(function: FunctionObj, query: QueryType) {
    println("==================")
    println("Function: $function")
    println("Query: $query")
    val result = when (val fitResult = fitsQuery(query, function)) {
        null -> "Failed to match function with query"
        else -> fitResult.toString()
    }
    println(result)
    println("==================")
}
