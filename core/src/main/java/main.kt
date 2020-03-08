val objType = directType("Object")

val strType = directType("String", objType)
val intType = directType("Integer", objType)
val fooType = directType("Foo", objType)

val collectionType = typeTemplate(
    name = "Collection",
    typeParams = listOf("E"),
    superType = listOf(objType)
)

val supplierType = typeTemplate(
    name = "Supplier",
    typeParams = listOf("S"),
    superType = listOf(objType)
)

val refType = typeTemplate(
    name = "Reference",
    typeParams = listOf("R"),
    superType = listOf(
        objType,
        supplierType.forceDynamicApply(
            "S" to ApplicationParameter.ParamSubstitution("R")
        )
    )
)

val pairType = typeTemplate(
    name = "Pair",
    typeParams = listOf("F", "S"),
    superType = listOf(objType)
)

val listType = Type.GenericType.TypeTemplate(
    name = "List",
    typeParams = listOf("T"),
    superTypes = listOf(
        SuperType.DynamicSuper(collectionType.forceDynamicApply(
            mapOf("E" to ApplicationParameter.ParamSubstitution("T"))
        ))
    )
)

fun main() {
    printType(objType)
    printType(strType)
    printType(intType)
    printType(fooType)
    printType(pairType)
    printType(refType)
    printType(collectionType)

    printType(pairType.forceStaticApply("F" to strType, "S" to fooType))
    printType(listType)
    printType(listType.forceApply(mapOf("T" to ApplicationParameter.StaticTypeSubstitution(strType))))

    val appliedList = listType.forceDynamicApply(mapOf("T" to ApplicationParameter.ParamSubstitution("V")))
    printType(appliedList)

    val listRefType = Type.GenericType.TypeTemplate(
        name = "ListRef",
        typeParams = listOf("V"),
        superTypes = listOf(
            SuperType.DynamicSuper(refType.forceDynamicApply(
                mapOf(
                    "R" to ApplicationParameter.DynamicTypeSubstitution(
                        listType.forceDynamicApply(mapOf("T" to ApplicationParameter.ParamSubstitution("V")))
                    )
                ))),
            SuperType.DynamicSuper(listType.forceDynamicApply(mapOf("T" to ApplicationParameter.ParamSubstitution("V"))))
        )
    )
    printType(listRefType)

    val mapType = Type.GenericType.TypeTemplate(
        name = "Map",
        typeParams = listOf("K", "V"),
        superTypes = listOf(
            SuperType.DynamicSuper(
                collectionType.forceDynamicApply(mapOf("E" to ApplicationParameter.DynamicTypeSubstitution(
                    pairType.forceDynamicApply(mapOf(
                        "F" to ApplicationParameter.ParamSubstitution("K"),
                        "S" to ApplicationParameter.ParamSubstitution("V")
                    ))
                )))
            )
        )
    )
    printType(mapType)

    val reqMapType = Type.GenericType.TypeTemplate(
        name = "ReqMap",
        typeParams = listOf("V"),
        superTypes = listOf(
            SuperType.DynamicSuper(
                mapType.forceDynamicApply(mapOf(
                    "K" to ApplicationParameter.StaticTypeSubstitution(
                        Type.NonGenericType.DirectType("Request", listOf(SuperType.StaticSuper(objType)))
                    ),
                    "V" to ApplicationParameter.ParamSubstitution("V")
                ))
            )
        )
    )
    printType(reqMapType)

    val ttlMapType = directType(
        "TtlMap",
        reqMapType.forceStaticApply("V" to intType)
    )
    printType(ttlMapType)
}
