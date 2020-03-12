
private object TypePrintConst {
    const val pre = "  ├──"
    const val emptyPre = "     "
    const val preLast = "  └──"
    const val preSibling = "  │  "
}

fun printType(type: Type) {
    val info = when (type) {
        is Type.NonGenericType.DirectType -> "Direct Type"
        is Type.NonGenericType.StaticAppliedType -> "Statically Applied Type"
        is Type.GenericType.TypeTemplate -> "Type template"
        is Type.GenericType.DynamicAppliedType -> "Dynamically Applied Type"
    }
    println("$info ${type.info}")

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