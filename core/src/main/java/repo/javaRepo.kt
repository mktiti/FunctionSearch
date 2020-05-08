package repo

import ApplicationParameter
import EnumMap
import PrimitiveType
import Type.NonGenericType
import Type.NonGenericType.*
import Type.DynamicAppliedType
import PrimitiveType.*
import TypeTemplate
import forceDynamicApply
import forceStaticApply

interface JavaRepo {

    val objectType: DirectType

    val voidType: DirectType

    fun primitive(primitive: PrimitiveType): DirectType

    fun boxed(primitive: PrimitiveType): DirectType

    fun arrayOf(type: NonGenericType): StaticAppliedType

    fun arrayOf(arg: ApplicationParameter): DynamicAppliedType

}

class IfoBasedJavaRepo(
        override val objectType: DirectType,
        override val voidType: DirectType,
        private val arrayTemplate: TypeTemplate,
        private val primitiveMap: EnumMap<PrimitiveType, DirectType>,
        private val boxedMap: EnumMap<PrimitiveType, DirectType>
) : JavaRepo {

    override fun primitive(primitive: PrimitiveType) = primitiveMap[primitive]

    override fun boxed(primitive: PrimitiveType): DirectType = boxedMap[primitive]

    override fun arrayOf(type: NonGenericType): StaticAppliedType {
        return arrayTemplate.forceStaticApply(type)
    }

    override fun arrayOf(arg: ApplicationParameter): DynamicAppliedType {
        return arrayTemplate.forceDynamicApply(arg)
    }
}

class FieldJavaRepo(
        override val objectType: DirectType,
        override val voidType: DirectType,
        val arrayTemplate: TypeTemplate,
        val boolType: DirectType,
        val byteType: DirectType,
        val charType: DirectType,
        val shortType: DirectType,
        val intType: DirectType,
        val longType: DirectType,
        val floatType: DirectType,
        val doubleType: DirectType
) : JavaRepo {

    override fun primitive(primitive: PrimitiveType) = when (primitive) {
        BOOL -> boolType
        BYTE -> byteType
        CHAR -> charType
        SHORT -> shortType
        INT -> intType
        LONG -> longType
        FLOAT -> floatType
        DOUBLE -> doubleType
    }

    override fun boxed(primitive: PrimitiveType): DirectType {
        TODO("Not yet implemented")
    }

    override fun arrayOf(type: NonGenericType): StaticAppliedType {
        return arrayTemplate.forceStaticApply(type)
    }

    override fun arrayOf(arg: ApplicationParameter): DynamicAppliedType {
        return arrayTemplate.forceDynamicApply(arg)
    }

}
