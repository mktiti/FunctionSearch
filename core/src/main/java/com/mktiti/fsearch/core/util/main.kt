package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.UPPER
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.core.util.show.TypePrint

val defaultRepo: MutableTypeRepo = SetTypeRepo(
        /*rootInfo = TypeInfo(
                name = "Object",
                packageName = emptyList(),
                artifact = "JCLv8"
        ),
        funTypeInfo = TypeInfo(
                name = "\$Fn",
                packageName = emptyList(),
                artifact = "JCLv8"
        )*/
)

val objType = defaultRepo.createDirect("Object")

val defaultTypeBounds = TypeBounds(
    upperBounds = setOf(TypeSubstitution(objType.holder()))
)

val charSeqType = defaultRepo.createDirect("CharSequence", objType)
val strType = defaultRepo.createDirect("String", charSeqType)
val intType = defaultRepo.createDirect("Integer", objType)
val fozType = defaultRepo.createDirect("Foo", objType)

val int64Type = defaultRepo.createDirect("Int64", intType)

val collectionType = defaultRepo.createTemplate(
    fullName = "Collection",
    typeParams = listOf(defaultRepo.typeParam("E", defaultTypeBounds)),
    superTypes = listOf(objType)
)

val supplierType = defaultRepo.createTemplate(
    fullName = "Supplier",
    typeParams = listOf(defaultRepo.typeParam("S", defaultTypeBounds)),
    superTypes = listOf(objType),
    samType = SamType.GenericSam(
            explicit = true,
            inputs = emptyList(),
            output = ParamSubstitution(0)
    )
)

val refType = defaultRepo.createTemplate(
    fullName = "Reference",
    typeParams = listOf(defaultRepo.typeParam("R", defaultTypeBounds)),
    superTypes = listOf(
            objType,
        supplierType.forceDynamicApply(ParamSubstitution(0))
    )
)

val pairType = defaultRepo.createTemplate(
    fullName = "Pair",
    typeParams = listOf(defaultRepo.typeParam("F", defaultTypeBounds), defaultRepo.typeParam("S", defaultTypeBounds)),
    superTypes = listOf(objType)
)

val listType = defaultRepo.createTemplate(
    fullName = "List",
    typeParams = listOf(defaultRepo.typeParam("T", defaultTypeBounds)),
    superTypes = listOf(
        collectionType.forceDynamicApply(ParamSubstitution(0))
    )
)

val linkedListType = defaultRepo.createTemplate(
    fullName = "LinkedList",
    typeParams = listOf(defaultRepo.typeParam("E", defaultTypeBounds)),
    superTypes = listOf(
        listType.forceDynamicApply(ParamSubstitution(0))
    )
)

fun main() {
    val resolver: TypeResolver = SingleRepoTypeResolver(defaultRepo)
    val fitter = JavaQueryFitter(resolver)

    val printer: TypePrint = JavaTypePrinter(resolver, MapJavaInfoRepo)

    printer.printType(objType)
    printer.printType(strType)
    printer.printType(intType)
    printer.printType(fozType)
    printer.printTypeTemplate(pairType)
    printer.printTypeTemplate(refType)
    printer.printTypeTemplate(collectionType)

    printer.printType(pairType.forceStaticApply(TypeHolder.staticDirects(strType, fozType)))
    printer.printTypeTemplate(listType)
    printer.printType(listType.forceStaticApply(strType.holder()))

    val appliedList = listType.forceDynamicApply(ParamSubstitution(0))
    printer.printType(appliedList)

    printer.printTypeTemplate(linkedListType)

    val listRefType = defaultRepo.createTemplate(
        fullName = "ListRef",
        typeParams = listOf(defaultRepo.typeParam("V", defaultTypeBounds)),
        superTypes = listOf(
            refType.forceDynamicApply(
                TypeSubstitution(
                    listType.forceDynamicApply(ParamSubstitution(0)).holder()
                )
            ),
            listType.forceDynamicApply(ParamSubstitution(0))
        )
    )
    printer.printTypeTemplate(listRefType)

    val mapType = defaultRepo.createTemplate(
        fullName = "Map",
        typeParams = listOf(defaultRepo.typeParam("K", defaultTypeBounds), defaultRepo.typeParam("V", defaultTypeBounds)),
        superTypes = listOf(
            collectionType.forceDynamicApply(
                TypeSubstitution(
                    pairType.forceDynamicApply(
                        ParamSubstitution(0),
                        ParamSubstitution(1)
                    ).holder()
                )
            )
        )
    )
    printer.printTypeTemplate(mapType)

    val reqMapType = defaultRepo.createTemplate(
        fullName = "ReqMap",
        typeParams = listOf(defaultRepo.typeParam("V", defaultTypeBounds)),
        superTypes = listOf(
            mapType.forceDynamicApply(
                StaticTypeSubstitution(
                    defaultRepo.createDirect("Request", objType).holder()
                ),
                ParamSubstitution(0)
            )
        )
    )
    printer.printTypeTemplate(reqMapType)

    val ttlMapType = defaultRepo.createDirect(
        "TtlMap",
        reqMapType.forceStaticApply(intType.holder())
    )
    printer.printType(ttlMapType)

    val fooType = defaultRepo.createTemplate(
        fullName = "Foo",
        typeParams = listOf(
                defaultRepo.typeParam("A", defaultTypeBounds),
                defaultRepo.typeParam("B", defaultTypeBounds),
                defaultRepo.typeParam("C", defaultTypeBounds)
        ), superTypes = listOf(objType)
    )
    printer.printTypeTemplate(fooType)

    val barSuperFoo = fooType.forceDynamicApply(
        ParamSubstitution(0),
        TypeSubstitution(
            pairType.forceDynamicApply(
                    ParamSubstitution(0),
                    ParamSubstitution(1)
            ).holder()
        ),
        ParamSubstitution(1)
    )

    val barType = defaultRepo.createTemplate(
        fullName = "Bar",
        typeParams = listOf(
                defaultRepo.typeParam("X", defaultTypeBounds),
                defaultRepo.typeParam("Y", defaultTypeBounds)
        ), superTypes = listOf(barSuperFoo)
    )
    printer.printTypeTemplate(barType)

    val appliedBar = barType.forceStaticApply(strType.holder(), intType.holder())
    printer.printType(appliedBar)

    val bazType = defaultRepo.createTemplate(
        fullName = "Baz",
        typeParams = listOf(
                defaultRepo.typeParam("K", defaultTypeBounds),
                defaultRepo.typeParam("V", defaultTypeBounds)
        ),
        superTypes = listOf(barType.forceDynamicApply(ParamSubstitution(1), ParamSubstitution(0)))
    )
    printer.printTypeTemplate(bazType)

    printer.printType(bazType.forceStaticApply(intType.holder(), objType.holder()))

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

    printer.printFit(fitter, lengthFun, QueryType(listOf(strType), intType))

    val functionType = defaultRepo.createTemplate(
            fullName = "Function",
            typeParams = listOf(
                    TypeParameter("I", defaultTypeBounds),
                    TypeParameter("O", defaultTypeBounds)
            ),
            superTypes = listOf(objType),
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
                            defaultRepo.typeParam("T", defaultTypeBounds),
                            defaultRepo.typeParam("R", defaultTypeBounds)
                    ),
                    inputParameters = listOf(
                            "list" to TypeSubstitution(listType.forceDynamicApply(ParamSubstitution(0)).holder()),
                            "mapper" to TypeSubstitution(
                                    functionType.forceDynamicApply(
                                            BoundedWildcard.Dynamic(ParamSubstitution(0), LOWER), // ? sup T
                                            BoundedWildcard.Dynamic(ParamSubstitution(1), UPPER)  // ? ext R
                                    ).holder()
                            )
                    ),
                    output = TypeSubstitution(listType.forceDynamicApply(ParamSubstitution(1)).holder()) // -> List<R>
            )
    )
    val appliedMapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType.holder()),
                    functionType.forceStaticApply(charSeqType.holder(), intType.holder())
            ),
            output = listType.forceStaticApply(intType.holder())
    )
    printer.printFit(fitter, mapFun, appliedMapQuery)

    val charSeqInt64MapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType.holder()),
                    functionType.forceStaticApply(charSeqType.holder(), int64Type.holder())
            ),
            output = listType.forceStaticApply(intType.holder())
    )
    printer.printFit(fitter, mapFun, charSeqInt64MapQuery)

    val wrongMapQuery = QueryType(
            inputParameters = listOf(
                    linkedListType.forceStaticApply(strType.holder()),
                    functionType.forceStaticApply(fozType.holder(), intType.holder())
            ),
            output = listType.forceStaticApply(intType.holder())
    )
    printer.printFit(fitter, mapFun, wrongMapQuery)

    val comparable = defaultRepo.createTemplate(
        fullName = "Comparable",
        typeParams = listOf(defaultRepo.typeParam("T", defaultTypeBounds)),
        superTypes = listOf(objType)
    )
    printer.printSemiType(comparable)

    fun comparableTo(name: String): TypeHolder.Static {
        return comparable.forceStaticApply(TypeHolder.staticIndirects(
                CompleteMinInfo.Static(
                        base = MinimalInfo(emptyList(), name),
                        args = emptyList()
                )
        )).holder()
    }

    val myStr = defaultRepo.createDirect(
        fullName = "MyComparableStr",
        superTypes = listOf(comparableTo("MyComparableStr"))
    )
    printer.printType(myStr)

    val personType = defaultRepo.createDirect(
        fullName = "Person",
        superTypes = listOf(comparableTo("Person"))
    )
    printer.printType(personType)

    val bossType = defaultRepo.createDirect(
        "Boss",
        personType
    )
    printer.printType(bossType)

    val sortedFun = FunctionObj(
            info = FunctionInfo(
                    name = "sorted",
                    fileName = ""
            ),
            signature = TypeSignature.GenericSignature(
                    typeParameters = listOf(
                            TypeParameter("T", upperBounds(
                                    TypeSubstitution(comparable.forceDynamicApply(BoundedWildcard.Dynamic(SelfSubstitution, LOWER)).holder())
                            ))
                    ),
                    inputParameters = listOf(
                            "collection" to TypeSubstitution(collectionType.forceDynamicApply(ParamSubstitution(0)).holder())
                    ),
                    output = TypeSubstitution(listType.forceDynamicApply(ParamSubstitution(0)).holder())
            )
    )

    val compStrSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(myStr.holder())),
            output = listType.forceStaticApply(myStr.holder())
    )
    printer.printFit(fitter, sortedFun, compStrSortQuery)

    val personSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(personType.holder())),
            output = listType.forceStaticApply(personType.holder())
    )
    printer.printFit(fitter, sortedFun, personSortQuery)


    val bossSortQuery = QueryType(
            inputParameters = listOf(listType.forceStaticApply(bossType.holder())),
            output = listType.forceStaticApply(bossType.holder())
    )
    printer.printFit(fitter, sortedFun, bossSortQuery)
}
