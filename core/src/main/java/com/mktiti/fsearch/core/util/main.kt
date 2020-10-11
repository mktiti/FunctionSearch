package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.MutableTypeRepo
import com.mktiti.fsearch.core.repo.SetTypeRepo
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection.UPPER
import com.mktiti.fsearch.core.type.SamType
import com.mktiti.fsearch.core.type.TypeInfo
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.upperBounds

val defaultRepo: MutableTypeRepo = SetTypeRepo(
        rootInfo = TypeInfo(
                name = "Object",
                packageName = emptyList(),
                artifact = "JCLv8"
        )/*,
        funTypeInfo = TypeInfo(
                name = "\$Fn",
                packageName = emptyList(),
                artifact = "JCLv8"
        )*/
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
    superTypes = listOf(objType),
    samType = SamType.GenericSam(
            explicit = true,
            inputs = emptyList(),
            output = ParamSubstitution(0)
    )
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
    val resolver: TypeResolver = SingleRepoTypeResolver(defaultRepo)
    val fitter = JavaQueryFitter(resolver)

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
                    listType.forceDynamicApply(ParamSubstitution(0)).type
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
                    ).type
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
                    defaultRepo.createDirect("Request", objType).completeInfo
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
            pairType.forceDynamicApply(ParamSubstitution(0), ParamSubstitution(1)).type
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

    printFit(fitter, lengthFun, QueryType(listOf(strType), intType))

    val functionType = defaultRepo.createTemplate(
            fullName = "Function",
            typeParams = listOf(
                    TypeParameter("I", defaultRepo.defaultTypeBounds),
                    TypeParameter("O", defaultRepo.defaultTypeBounds)
            ),
            superTypes = listOf(defaultRepo.rootType),
            samType = SamType.GenericSam(
                    explicit = true,
                    inputs = listOf(ParamSubstitution(0)),
                    output = ParamSubstitution(1)
            )
    )

    val mapFun = FunctionObj(
            info = FunctionInfo("map", "List"),
            signature = TypeSignature.GenericSignature(
                    typeParameters = listOf(
                            defaultRepo.typeParam("T", defaultRepo.defaultTypeBounds),
                            defaultRepo.typeParam("R", defaultRepo.defaultTypeBounds)
                    ),
                    inputParameters = listOf(
                            "list" to DynamicTypeSubstitution(listType.forceDynamicApply(ParamSubstitution(0)).type),
                            "mapper" to DynamicTypeSubstitution(
                                    functionType.forceDynamicApply(
                                            Wildcard.Bounded(ParamSubstitution(0), LOWER), // ? sup T
                                            Wildcard.Bounded(ParamSubstitution(1), UPPER)  // ? ext R
                                    ).type
                            )
                    ),
                    output = DynamicTypeSubstitution(listType.forceDynamicApply(ParamSubstitution(1)).type) // -> List<R>
            )
    )
    val appliedMapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType),
                    functionType.forceStaticApply(charSeqType, intType)
            ),
            output = listType.forceStaticApply(intType)
    )
    printFit(fitter, mapFun, appliedMapQuery)

    val charSeqInt64MapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType),
                    functionType.forceStaticApply(charSeqType, int64Type)
            ),
            output = listType.forceStaticApply(intType)
    )
    printFit(fitter, mapFun, charSeqInt64MapQuery)

    val wrongMapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType),
                    functionType.forceStaticApply(fozType, intType)
            ),
            output = listType.forceStaticApply(intType)
    )
    printFit(fitter, mapFun, wrongMapQuery)

    val comparable = defaultRepo.createTemplate(
        fullName = "Comparable",
        typeParams = listOf(defaultRepo.typeParam("T")),
        superTypes = listOf(defaultRepo.rootType)
    )
    printSemiType(comparable)

    val myStr = defaultRepo.createSelfRefDirect(
        fullName = "MyComparableStr",
        superCreators = listOf { self -> comparable.forceStaticApply(self) }
    )
    printType(myStr)

    val personType = defaultRepo.createSelfRefDirect(
        fullName = "Person",
        superCreators = listOf { self -> comparable.forceStaticApply(self) }
    )
    printType(personType)

    val bossType = defaultRepo.createDirect(
        "Boss",
        personType
    )
    printType(bossType)

    val sortedFun = FunctionObj(
            info = FunctionInfo(
                    name = "sorted",
                    fileName = ""
            ),
            signature = TypeSignature.GenericSignature(
                    typeParameters = listOf(
                            TypeParameter("T", upperBounds(
                                    DynamicTypeSubstitution(comparable.forceDynamicApply(Wildcard.Bounded(SelfSubstitution, LOWER)).type)
                            ))
                    ),
                    inputParameters = listOf(
                            "collection" to DynamicTypeSubstitution(collectionType.forceDynamicApply(ParamSubstitution(0)).type)
                    ),
                    output = DynamicTypeSubstitution(listType.forceDynamicApply(ParamSubstitution(0)).type)
            )
    )

    val compStrSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(myStr)),
            output = listType.forceStaticApply(myStr)
    )
    printFit(fitter, sortedFun, compStrSortQuery)

    val personSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(personType)),
            output = listType.forceStaticApply(personType)
    )
    printFit(fitter, sortedFun, personSortQuery)


    val bossSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(bossType)),
            output = listType.forceStaticApply(bossType)
    )
    printFit(fitter, sortedFun, bossSortQuery)
}
