/*
fun main(args: Array<String>) {
    val obj = Type.DirectType(TypeInfo(artifact = "JCLv8", decPackage = "java.lang", name = "Object"), superTypes = emptyList())

    val collection = Type.GenericType(
        info = TypeInfo(decPackage = "java.collection", name = "Collection"),
        typeParams = listOf(
            TypeParameter(0, "T", emptyList())
        ),
        superTypes = listOf(TypeInheritance.ConcreteInheritance(obj))
    )

    val pair = Type.GenericType(
        info = TypeInfo(decPackage = "java.util", name = "Pair"),
        typeParams = listOf(
            TypeParameter(0, "F", emptyList()),
            TypeParameter(1, "S", emptyList())
        ),
        superTypes = listOf(TypeInheritance.ConcreteInheritance(obj))
    )

    val comp = Type.DirectType(TypeInfo(name = "Comp", decPackage = "java.ord"), superTypes = emptyList())

    val map = Type.GenericType(
        info = TypeInfo(decPackage = "java.collection", name = "Map"),
        typeParams = listOf(
            TypeParameter(0, "K", listOf(TypeParamRestriction.LowerBound(comp))),
            TypeParameter(1, "V", emptyList())
        ),
        superTypes = listOf(TypeInheritance.GenericInheritance(collection, paramMap = listOf(
            TypeParamMapping.ParamSubstitute("V", "V")
        )))
    )

    val ord = Type.DirectType(TypeInfo(name = "Ord", decPackage = "java.ord"), superTypes = emptyList())

    val sortedMap = Type.GenericType(
        info = TypeInfo(decPackage = "java.collection", name = "SortedMap"),
        typeParams = listOf(
            TypeParameter(0, "K", listOf(TypeParamRestriction.LowerBound(ord), TypeParamRestriction.LowerBound(comp))),
            TypeParameter(1, "V", emptyList())
    ),
        superTypes = listOf(TypeInheritance.GenericInheritance(map, paramMap = listOf(
            TypeParamMapping.ParamSubstitute("K", "K"),
            TypeParamMapping.ParamSubstitute("V", "V")
        )))
    )

    val req = Type.DirectType(TypeInfo(name = "Request", decPackage = "java.net"), superTypes = emptyList())
    val int = Type.DirectType(TypeInfo(name = "Int", decPackage = "java.lang"), superTypes = emptyList())

    val reqMap = Type.GenericType(
        info = TypeInfo(decPackage = "java.net", name = "ReqMap"),
        typeParams = listOf(
            TypeParameter(0, "V", emptyList())
        ),
        superTypes = listOf(TypeInheritance.GenericInheritance(sortedMap, paramMap = listOf(
            TypeParamMapping.TypeSubstitute("K", req),
            TypeParamMapping.ParamSubstitute("V", "V")
        )))
    )

    val ttlMap = Type.DirectType(
        info = TypeInfo(artifact = "MyArt", decPackage = "com.mktiti", name = "TtlMap"),
        superTypes = listOf(TypeInheritance.GenericInheritance(reqMap, paramMap = listOf(
            TypeParamMapping.TypeSubstitute("V", int)
        )))
    )

    printType(obj)
    printType(collection)
    printType(pair)
    printType(map)
    printType(ord)
    printType(sortedMap)
    printType(req)
    printType(int)
    printType(reqMap)
    printType(ttlMap)

    /* val runnable = nonGenericType("JCLv8", "java.lang", "Runnable")

    val list = Type(
        info = TypeInfo("List", decPackage = "java.collection"),
        typeParams = listOf(
            TypeParameter(0, "T", emptyList())
        )
    )

    val linked = Type(
        info = TypeInfo("LinkedList", decPackage = "java.collection"),
        typeParams = listOf(
            TypeParameter(0, "E", emptyList())
        )
    )

    val serviceList = Type(
        info = TypeInfo("ServiceList", decPackage = "com.mktiti", artifact = "MyArt"),
        typeParams = listOf(
            TypeParameter(id = 0, sign = "S", restrictions = listOf(
                TypeParamRestriction.LowerBound(bound = runnable)
            ))
        )
    )
     */

    // println(obj)



    /*
    println(list)
    println(linked)
    println(runnable)
    println(serviceList)
     */

}
 */
