val objType = directType("Object")

val strType = directType("String", objType)
val intType = directType("Integer", objType)
val fooType = directType("Foo", objType)

val collectionType = typeTemplate(
    name = "Collection",
    typeParams = listOf("E"),
    superTypes = listOf(objType)
)

val supplierType = typeTemplate(
    name = "Supplier",
    typeParams = listOf("S"),
    superTypes = listOf(objType)
)

val refType = typeTemplate(
    name = "Reference",
    typeParams = listOf("R"),
    superTypes = listOf(
        objType,
        supplierType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0))
    )
)

val pairType = typeTemplate(
    name = "Pair",
    typeParams = listOf("F", "S"),
    superTypes = listOf(objType)
)

val listType = typeTemplate(
    name = "List",
    typeParams = listOf("T"),
    superTypes = listOf(
        collectionType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0))
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

    printType(pairType.forceStaticApply(strType, fooType))
    printType(listType)
    printType(listType.forceStaticApply(strType))

    val appliedList = listType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0))
    printType(appliedList)

    val listRefType = typeTemplate(
        name = "ListRef",
        typeParams = listOf("V"),
        superTypes = listOf(
            refType.forceDynamicApply(
                ApplicationParameter.DynamicTypeSubstitution(
                    listType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0))
                )
            ),
            listType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0))
        )
    )
    printType(listRefType)

    val mapType = typeTemplate(
        name = "Map",
        typeParams = listOf("K", "V"),
        superTypes = listOf(
            collectionType.forceDynamicApply(
                ApplicationParameter.DynamicTypeSubstitution(
                    pairType.forceDynamicApply(
                        ApplicationParameter.ParamSubstitution(0),
                        ApplicationParameter.ParamSubstitution(1)
                    )
                )
            )
        )
    )
    printType(mapType)

    val reqMapType = typeTemplate(
        name = "ReqMap",
        typeParams = listOf("V"),
        superTypes = listOf(
            mapType.forceDynamicApply(
                ApplicationParameter.StaticTypeSubstitution(
                        directType("Request", objType)
                ),
                ApplicationParameter.ParamSubstitution(0)
            )
        )
    )
    printType(reqMapType)

    val ttlMapType = directType(
        "TtlMap",
        reqMapType.forceStaticApply(intType)
    )
    printType(ttlMapType)

    val fooType = typeTemplate(
        name = "Foo",
        typeParams = listOf("A", "B", "C"),
        superTypes = listOf(objType)
    )
    printType(fooType)

    val barSuperFoo = fooType.forceDynamicApply(
        ApplicationParameter.ParamSubstitution(0),
        ApplicationParameter.DynamicTypeSubstitution(
            pairType.forceDynamicApply(ApplicationParameter.ParamSubstitution(0), ApplicationParameter.ParamSubstitution(1))
        ),
        ApplicationParameter.ParamSubstitution(1)
    )

    val barType = typeTemplate(
        name = "Bar",
        typeParams = listOf("X", "Y"),
        superTypes = listOf(barSuperFoo)
    )
    printType(barType)

    val appliedBar = barType.forceStaticApply(strType, intType)
    printType(appliedBar)

    val bazType = typeTemplate(
        name = "Baz",
        typeParams = listOf("K", "V"),
        superTypes = listOf(barType.forceDynamicApply(ApplicationParameter.ParamSubstitution(1), ApplicationParameter.ParamSubstitution(0)))
    )
    printType(bazType)

    printType(bazType.forceStaticApply(intType, objType))
}
