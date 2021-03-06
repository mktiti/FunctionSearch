package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeHolder

interface JavaRepo {

    val objectType: TypeHolder.Static

    val voidType: TypeHolder.Static

    fun primitive(primitive: PrimitiveType): TypeHolder.Static.Direct

    fun boxed(primitive: PrimitiveType): TypeHolder.Static

    fun arrayOf(type: TypeHolder.Static): Type.NonGenericType.StaticAppliedType

    fun arrayOf(arg: ApplicationParameter): Type.DynamicAppliedType

}
