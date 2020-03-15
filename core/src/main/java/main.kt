import ApplicationParameter.*

val defaultRepo: MutableTypeRepo = SetTypeRepo()

val objType = defaultRepo.createDirect("Object")

val charSeqType = defaultRepo.createDirect("CharSequence", objType)
val strType = defaultRepo.createDirect("String", charSeqType)
val intType = defaultRepo.createDirect("Integer", objType)
val fooType = defaultRepo.createDirect("Foo", objType)

val int64Type = defaultRepo.createDirect("Int64", intType)

val collectionType = defaultRepo.createTemplate(
    fullName = "Collection",
    typeParams = listOf(TypeParameter("E")),
    superTypes = listOf(objType)
)

val supplierType = defaultRepo.createTemplate(
    fullName = "Supplier",
    typeParams = listOf(TypeParameter("S")),
    superTypes = listOf(objType)
)

val refType = defaultRepo.createTemplate(
    fullName = "Reference",
    typeParams = listOf(TypeParameter("R")),
    superTypes = listOf(
        objType,
        supplierType.forceDynamicApply(ParamSubstitution(0))
    )
)

val pairType = defaultRepo.createTemplate(
    fullName = "Pair",
    typeParams = listOf(TypeParameter("F"), TypeParameter("S")),
    superTypes = listOf(objType)
)

val listType = defaultRepo.createTemplate(
    fullName = "List",
    typeParams = listOf(TypeParameter("T")),
    superTypes = listOf(
        collectionType.forceDynamicApply(ParamSubstitution(0))
    )
)

val linkedListType = defaultRepo.createTemplate(
    fullName = "LinkedList",
    typeParams = listOf(TypeParameter("E")),
    superTypes = listOf(
        listType.forceDynamicApply(ParamSubstitution(0))
    )
)

fun main() {
    printType(objType)
    printType(strType)
    printType(intType)
    printType(fooType)
    printTypeTemplate(pairType)
    printTypeTemplate(refType)
    printTypeTemplate(collectionType)

    printType(pairType.forceStaticApply(strType, fooType))
    printTypeTemplate(listType)
    printType(listType.forceStaticApply(strType))

    val appliedList = listType.forceDynamicApply(ParamSubstitution(0))
    printType(appliedList)

    printTypeTemplate(linkedListType)

    val listRefType = defaultRepo.createTemplate(
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
    printTypeTemplate(listRefType)

    val mapType = defaultRepo.createTemplate(
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
    printTypeTemplate(mapType)

    val reqMapType = defaultRepo.createTemplate(
        fullName = "ReqMap",
        typeParams = listOf(TypeParameter("V")),
        superTypes = listOf(
            mapType.forceDynamicApply(
                StaticTypeSubstitution(
                        defaultRepo.createDirect("Request", objType)
                ),
                ParamSubstitution(0)
            )
        )
    )
    printTypeTemplate(reqMapType)

    val ttlMapType = defaultRepo.createDirect(
        "TtlMap",
        reqMapType.forceStaticApply(intType)
    )
    printType(ttlMapType)

    val fooType = defaultRepo.createTemplate(
        fullName = "Foo",
        typeParams = listOf(TypeParameter("A"), TypeParameter("B"), TypeParameter("C")),
        superTypes = listOf(objType)
    )
    printTypeTemplate(fooType)

    val barSuperFoo = fooType.forceDynamicApply(
        ParamSubstitution(0),
        DynamicTypeSubstitution(
            pairType.forceDynamicApply(ParamSubstitution(0), ParamSubstitution(1))
        ),
        ParamSubstitution(1)
    )

    val barType = defaultRepo.createTemplate(
        fullName = "Bar",
        typeParams = listOf(TypeParameter("X"), TypeParameter("Y")),
        superTypes = listOf(barSuperFoo)
    )
    printTypeTemplate(barType)

    val appliedBar = barType.forceStaticApply(strType, intType)
    printType(appliedBar)

    val bazType = defaultRepo.createTemplate(
        fullName = "Baz",
        typeParams = listOf(TypeParameter("K"), TypeParameter("V", bounds = TypeBounds(upperBounds = setOf(objType)))),
        superTypes = listOf(barType.forceDynamicApply(ParamSubstitution(1), ParamSubstitution(0)))
    )
    printTypeTemplate(bazType)

    printType(bazType.forceStaticApply(intType, objType))

    val funType = defaultRepo.createTemplate(
        fullName = "Function",
        typeParams = listOf(TypeParameter("I"), TypeParameter("O")),
        superTypes = listOf(objType)
    )
    printTypeTemplate(funType)

    val maxFun = Function(
        info = FunctionInfo("max", "Math"),
        signature = TypeSignature.DirectSignature(
            inputParameters = listOf("a" to intType, "b" to intType),
            output = intType
        )
    )
    println(maxFun)

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

    println("===============")
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
    val appliedMapQuery = directQuery(
        inputParameters = listOf(
            linkedListType.forceStaticApply(strType),
            funType.forceStaticApply(strType, intType)
        ),
        output = listType.forceStaticApply(intType)
    )

    println(mapFun)
    println("Query: ${appliedMapQuery.fullString}")
    println("Fit: " + fitsQuery(appliedMapQuery, mapFun))
}
