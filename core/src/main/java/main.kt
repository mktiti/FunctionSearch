import ApplicationParameter.ParamSubstitution
import ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution

val defaultRepo: MutableTypeRepo = SetTypeRepo(
    rootInfo = TypeInfo(
        name = "Object",
        packageName = "",
        artifact = "JCLv8"
    ),
    funTypeInfo = TypeInfo(
        name = "\$Fn",
        packageName = "",
        artifact = "JCLv8"
    )
)

val objType = defaultRepo.createDirect("Object")

val charSeqType = defaultRepo.createDirect("CharSequence", objType)
val strType = defaultRepo.createDirect("String", charSeqType)
val intType = defaultRepo.createDirect("Integer", objType)
val fozType = defaultRepo.createDirect("Foo", objType)

val int64Type = defaultRepo.createDirect("Int64", intType)

val collectionType = defaultRepo.createTemplate(
    fullName = "Collection",
    typeParams = listOf(defaultRepo.typeParam("E")),
    superTypes = listOf(objType)
)

val supplierType = defaultRepo.createTemplate(
    fullName = "Supplier",
    typeParams = listOf(defaultRepo.typeParam("S")),
    superTypes = listOf(objType)
)

val refType = defaultRepo.createTemplate(
    fullName = "Reference",
    typeParams = listOf(defaultRepo.typeParam("R")),
    superTypes = listOf(
        objType,
        supplierType.forceDynamicApply(ParamSubstitution(0))
    )
)

val pairType = defaultRepo.createTemplate(
    fullName = "Pair",
    typeParams = listOf(defaultRepo.typeParam("F"), defaultRepo.typeParam("S")),
    superTypes = listOf(objType)
)

val listType = defaultRepo.createTemplate(
    fullName = "List",
    typeParams = listOf(defaultRepo.typeParam("T")),
    superTypes = listOf(
        collectionType.forceDynamicApply(ParamSubstitution(0))
    )
)

val linkedListType = defaultRepo.createTemplate(
    fullName = "LinkedList",
    typeParams = listOf(defaultRepo.typeParam("E")),
    superTypes = listOf(
        listType.forceDynamicApply(ParamSubstitution(0))
    )
)

fun main() {
    printType(objType)
    printType(strType)
    printType(intType)
    printType(fozType)
    printTypeTemplate(pairType)
    printTypeTemplate(refType)
    printTypeTemplate(collectionType)

    printType(pairType.forceStaticApply(strType, fozType))
    printTypeTemplate(listType)
    printType(listType.forceStaticApply(strType))

    val appliedList = listType.forceDynamicApply(ParamSubstitution(0))
    printType(appliedList)

    printTypeTemplate(linkedListType)

    val listRefType = defaultRepo.createTemplate(
        fullName = "ListRef",
        typeParams = listOf(defaultRepo.typeParam("V")),
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
        typeParams = listOf(defaultRepo.typeParam("K"), defaultRepo.typeParam("V")),
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
        typeParams = listOf(defaultRepo.typeParam("V")),
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
        typeParams = listOf(defaultRepo.typeParam("A"), defaultRepo.typeParam("B"), defaultRepo.typeParam("C")),
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
        typeParams = listOf(defaultRepo.typeParam("X"), defaultRepo.typeParam("Y")),
        superTypes = listOf(barSuperFoo)
    )
    printTypeTemplate(barType)

    val appliedBar = barType.forceStaticApply(strType, intType)
    printType(appliedBar)

    val bazType = defaultRepo.createTemplate(
        fullName = "Baz",
        typeParams = listOf(defaultRepo.typeParam("K"), defaultRepo.typeParam("V")),
        superTypes = listOf(barType.forceDynamicApply(ParamSubstitution(1), ParamSubstitution(0)))
    )
    printTypeTemplate(bazType)

    printType(bazType.forceStaticApply(intType, objType))

    val maxFun = FunctionObj(
        info = FunctionInfo("max", "Math"),
        signature = TypeSignature.DirectSignature(
            inputParameters = listOf("a" to intType, "b" to intType),
            output = intType
        )
    )
    println(maxFun)

    val lengthFun = FunctionObj(
        info = FunctionInfo("length", "String"),
        signature = TypeSignature.DirectSignature(
            inputParameters = listOf("string" to charSeqType),
            output = int64Type
        )
    )
    println(lengthFun)

    printFit(lengthFun, QueryType(listOf(strType), intType))

    val mapFun = FunctionObj(
        info = FunctionInfo("map", "List"),
        signature = TypeSignature.GenericSignature(
            typeParameters = listOf( // <T, R, ? sup T, ? ext R>
                defaultRepo.typeParam("T", defaultRepo.defaultTypeBounds),
                defaultRepo.typeParam("R", defaultRepo.defaultTypeBounds),
                TypeParameter("\$supT", lowerBounds(ParamSubstitution(0))),
                TypeParameter("\$extR", upperBounds(ParamSubstitution(1)))
            ),
            inputParameters = listOf( // (list: List<T>, mapper: Fn<? sup T, ? ext R>)
                "list" to DynamicTypeSubstitution(listType.forceDynamicApply(ParamSubstitution(0))),
                "mapper" to DynamicTypeSubstitution(
                    defaultRepo.functionType(1).forceDynamicApply(
                        ParamSubstitution(2), // ? sup T
                        ParamSubstitution(3)  // ? ext R
                    )
                )
            ),
            output = DynamicTypeSubstitution(listType.forceDynamicApply(ParamSubstitution(1))) // -> List<R>
        )
    )
    val appliedMapQuery = QueryType(
        inputParameters = listOf(
            linkedListType.forceStaticApply(strType),
            defaultRepo.functionType(1).forceStaticApply(charSeqType, intType)
        ),
        output = listType.forceStaticApply(intType)
    )
    printFit(mapFun, appliedMapQuery)

    val charSeqInt64MapQuery = QueryType(
        inputParameters = listOf(
            linkedListType.forceStaticApply(strType),
            defaultRepo.functionType(1).forceStaticApply(charSeqType, int64Type)
        ),
        output = listType.forceStaticApply(intType)
    )
    printFit(mapFun, charSeqInt64MapQuery)

    val wrongMapQuery = QueryType(
        inputParameters = listOf(
            linkedListType.forceStaticApply(strType),
            defaultRepo.functionType(1).forceStaticApply(fozType, intType)
        ),
        output = listType.forceStaticApply(intType)
    )
    printFit(mapFun, wrongMapQuery)
}
