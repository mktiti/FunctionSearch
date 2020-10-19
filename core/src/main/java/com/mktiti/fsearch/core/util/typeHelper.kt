package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType

@Suppress("UNUSED")
fun directType(fullName: String, vararg superType: NonGenericType) = DirectType(
    minInfo = info(fullName).minimal,
    superTypes = TypeHolder.staticDirects(superType.toList()),
    samType = null,
    virtual = false
)

@Suppress("UNUSED")
fun typeTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type<*>>) = TypeTemplate(
        info = info(fullName).minimal,
        typeParams = typeParams,
        superTypes = TypeHolder.anyDirects(superTypes),
        samType = null
)

fun TypeApplicable.forceStaticApply(typeArgs: List<TypeHolder.Static>): StaticAppliedType
        = staticApply(typeArgs) ?: throw TypeApplicationException("Failed to static apply type args $typeArgs to $this")

fun TypeApplicable.forceStaticApply(vararg typeArgs: TypeHolder.Static): StaticAppliedType
        = forceStaticApply(typeArgs.toList())

fun TypeApplicable.forceDynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType
        = dynamicApply(typeArgs) ?: throw TypeApplicationException("Failed to dynamically apply type args $typeArgs to $this")

fun TypeApplicable.forceDynamicApply(vararg typeArgs: ApplicationParameter): DynamicAppliedType
        = forceDynamicApply(typeArgs.toList())

fun TypeApplicable.forceApply(typeArgs: List<ApplicationParameter>): Type<*>
        = apply(typeArgs) ?: throw TypeApplicationException("Failed to apply type args $typeArgs to $this")
