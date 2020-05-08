package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType

interface JavaRepo {

    val objectType: DirectType

    val voidType: DirectType

    fun primitive(primitive: PrimitiveType): DirectType

    fun boxed(primitive: PrimitiveType): DirectType

    fun arrayOf(type: NonGenericType): StaticAppliedType

    fun arrayOf(arg: ApplicationParameter): DynamicAppliedType

}
