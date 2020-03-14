import ApplicationParameter.*

val objType = directType("Object")

val charSeqType = directType("CharSequence", objType)
val strType = directType("String", charSeqType)
val intType = directType("Integer", objType)
val fooType = directType("Foo", objType)

val int64Type = directType("Int64", intType)

val collectionType = typeTemplate(
    fullName = "Collection",
    typeParams = listOf(TypeParameter("E")),
    superTypes = listOf(objType)
)

val supplierType = typeTemplate(
    fullName = "Supplier",
    typeParams = listOf(TypeParameter("S")),
    superTypes = listOf(objType)
)

val refType = typeTemplate(
    fullName = "Reference",
    typeParams = listOf(TypeParameter("R")),
    superTypes = listOf(
        objType,
        supplierType.forceDynamicApply(ParamSubstitution(0))
    )
)

val pairType = typeTemplate(
    fullName = "Pair",
    typeParams = listOf(TypeParameter("F"), TypeParameter("S")),
    superTypes = listOf(objType)
)

val listType = typeTemplate(
    fullName = "List",
    typeParams = listOf(TypeParameter("T")),
    superTypes = listOf(
        collectionType.forceDynamicApply(ParamSubstitution(0))
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

    val appliedList = listType.forceDynamicApply(ParamSubstitution(0))
    printType(appliedList)

    val listRefType = typeTemplate(
        fullName = "ListRef",
        typeParams = listOf(TypeParameter("V")),
        superTypes = listOf(
            refType.forceDynamicApply(
                DynamicTypeSubstitution(
                    listType.forceDynamicApply(ParamSubstitution(0))
                )
            ),
            listType.forceDynamicApply(ParamSubstitution(0))
        )
    )
    printType(listRefType)

    val mapType = typeTemplate(
        fullName = "Map",
        typeParams = listOf(TypeParameter("K"), TypeParameter("V")),
        superTypes = listOf(
            collectionType.forceDynamicApply(
                DynamicTypeSubstitution(
                    pairType.forceDynamicApply(
                        ParamSubstitution(0),
                        ParamSubstitution(1)
                    )
                )
            )
        )
    )
    printType(mapType)

    val reqMapType = typeTemplate(
        fullName = "ReqMap",
        typeParams = listOf(TypeParameter("V")),
        superTypes = listOf(
            mapType.forceDynamicApply(
                StaticTypeSubstitution(
                        directType("Request", objType)
                ),
                ParamSubstitution(0)
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
        fullName = "Foo",
        typeParams = listOf(TypeParameter("A"), TypeParameter("B"), TypeParameter("C")),
        superTypes = listOf(objType)
    )
    printType(fooType)

    val barSuperFoo = fooType.forceDynamicApply(
        ParamSubstitution(0),
        DynamicTypeSubstitution(
            pairType.forceDynamicApply(ParamSubstitution(0), ParamSubstitution(1))
        ),
        ParamSubstitution(1)
    )

    val barType = typeTemplate(
        fullName = "Bar",
        typeParams = listOf(TypeParameter("X"), TypeParameter("Y")),
        superTypes = listOf(barSuperFoo)
    )
    printType(barType)

    val appliedBar = barType.forceStaticApply(strType, intType)
    printType(appliedBar)

    val bazType = typeTemplate(
        fullName = "Baz",
        typeParams = listOf(TypeParameter("K"), TypeParameter("V", upperBounds = listOf(objType))),
        superTypes = listOf(barType.forceDynamicApply(ParamSubstitution(1), ParamSubstitution(0)))
    )
    printType(bazType)

    printType(bazType.forceStaticApply(intType, objType))

    val funType = typeTemplate(
        fullName = "Function",
        typeParams = listOf(TypeParameter("I"), TypeParameter("O")),
        superTypes = listOf(objType)
    )
    printType(funType)

    val maxFun = Function(
        info = FunctionInfo("max", "Math"),
        signature = TypeSignature.DirectSignature(
            inputParameters = listOf("a" to intType, "b" to intType),
            output = intType
        )
    )
    println(maxFun)

    val mapFun = Function(
        info = FunctionInfo("map", "List"),
        signature = TypeSignature.GenericSignature(
            typeParameters = listOf(TypeParameter("T"), TypeParameter("R")),
            inputParameters = listOf(
                "list" to listType.forceDynamicApply(ParamSubstitution(0)),
                "mapper" to funType.forceDynamicApply(ParamSubstitution(0), ParamSubstitution(1))
            ),
            output = listType.forceDynamicApply(ParamSubstitution(1))
        )
    )
    println(mapFun)

    val lengthFun = Function(
        info = FunctionInfo("length", "String"),
        signature = TypeSignature.DirectSignature(
            inputParameters = listOf("string" to charSeqType),
            output = int64Type
        )
    )
    println(lengthFun)

    val lenQuery = directQuery(listOf(strType), intType)
    println("Query: ${lenQuery.fullString}")
    println("Fit: " + fitsQuery(lenQuery, lengthFun))
}
