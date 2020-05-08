package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.util.EnumMap

interface JavaInfoRepo {

    val objectType: MinimalInfo

    val voidType: MinimalInfo

    val arrayType: MinimalInfo

    fun primitive(primitive: PrimitiveType): MinimalInfo

    fun boxed(primitive: PrimitiveType): MinimalInfo

}

object MapJavaInfoRepo : JavaInfoRepo {

    private val internalPackage = MinimalInfo(listOf("\$internal"), "")
    private fun internal(name: String) = internalPackage.copy(simpleName = "\$$name")

    private val langPackage = MinimalInfo(listOf("java", "lang"), "")
    private fun inLang(name: String) = langPackage.copy(simpleName = name)

    private fun primitiveType(primitive: PrimitiveType) = internal(primitive.javaName)

    private fun boxInfo(primitive: PrimitiveType) = when (primitive) {
        PrimitiveType.CHAR -> inLang("Character")
        PrimitiveType.INT -> inLang("Integer")
        else -> inLang(primitive.javaName.capitalize())
    }

    override val objectType = inLang("Object")
    override val voidType = inLang("Void")
    override val arrayType = internal("Array")

    private val primitiveMap = EnumMap.eager(this::primitiveType)
    private val boxMap = EnumMap.eager(this::boxInfo)

    override fun primitive(primitive: PrimitiveType) = primitiveMap[primitive]

    override fun boxed(primitive: PrimitiveType) = boxMap[primitive]

}
