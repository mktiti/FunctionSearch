package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PackageName
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.util.EnumMap

interface JavaInfoRepo {

    val objectType: MinimalInfo

    val voidType: MinimalInfo

    val arrayType: MinimalInfo

    fun isInternal(info: MinimalInfo): Boolean

    fun primitive(primitive: PrimitiveType): MinimalInfo

    fun boxed(primitive: PrimitiveType): MinimalInfo

    fun ifPrimitive(info: MinimalInfo): PrimitiveType?

    fun ifBoxed(info: MinimalInfo): PrimitiveType?

    fun funInfo(inParamCount: Int): MinimalInfo

    fun ifFunParamCount(info: MinimalInfo): Int?

}

private class FunInfoHandler(
        private val packageName: PackageName,
        cacheMax: Int = 10
) {

    companion object {
        private const val prefix = "\$Fun_"
    }

    private val stored: List<MinimalInfo> = (0 .. cacheMax).map(this::createInfo)

    private fun createInfo(inParamCount: Int) = MinimalInfo(packageName, "$prefix$inParamCount", true)

    operator fun get(inParamCount: Int) = stored.getOrElse(inParamCount, this::createInfo)

    fun ifFunParamCount(info: MinimalInfo) = if (info.packageName == packageName && info.simpleName.startsWith(prefix)) {
        info.simpleName.drop(prefix.length).toIntOrNull()
    } else {
        null
    }

}

object MapJavaInfoRepo : JavaInfoRepo {

    private val internalPackage = listOf("\$internal")
    private fun internal(name: String) = MinimalInfo(simpleName = name, packageName = internalPackage, virtual = true)

    private val funInfoHandler = FunInfoHandler(internalPackage)

    private val langPackage = MinimalInfo(listOf("java", "lang"), "")
    private fun inLang(name: String) = langPackage.copy(simpleName = name)

    private fun primitiveType(primitive: PrimitiveType) = internal(primitive.javaName)

    private fun boxInfo(primitive: PrimitiveType) = when (primitive) {
        PrimitiveType.CHAR -> inLang("Character")
        PrimitiveType.INT -> inLang("Integer")
        else -> inLang(primitive.javaName.capitalize())
    }

    override val objectType = inLang("Object")
    override val voidType = internal("void")
    override val arrayType = internal("Array")

    private val primitiveMap = EnumMap.eager(this::primitiveType)
    private val boxMap = EnumMap.eager(this::boxInfo)

    override fun isInternal(info: MinimalInfo) = info.packageName == internalPackage

    override fun primitive(primitive: PrimitiveType) = primitiveMap[primitive]

    override fun ifPrimitive(info: MinimalInfo): PrimitiveType? = if (isInternal(info)) {
        PrimitiveType.values().find { it.javaName == info.simpleName }
    } else {
        null
    }

    override fun boxed(primitive: PrimitiveType) = boxMap[primitive]

    override fun ifBoxed(info: MinimalInfo): PrimitiveType? = PrimitiveType.values().mapNotNull {
        if (boxMap[it] == info) it else null
    }.firstOrNull()

    override fun funInfo(inParamCount: Int): MinimalInfo = funInfoHandler[inParamCount]

    override fun ifFunParamCount(info: MinimalInfo) = funInfoHandler.ifFunParamCount(info)
}
