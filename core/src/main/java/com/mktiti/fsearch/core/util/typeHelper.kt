package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.SuperType.DynamicSuper.EagerDynamic
import com.mktiti.fsearch.core.type.SuperType.StaticSuper.EagerStatic
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType

@Suppress("UNUSED")
fun directType(fullName: String, vararg superType: NonGenericType) = DirectType(
    info = info(fullName),
    superTypes = superType.map { EagerStatic(it) }
)

@Suppress("UNUSED")
fun typeTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type>) = TypeTemplate(
        info = info(fullName),
        typeParams = typeParams,
        superTypes = superTypes.map {
            when (it) {
                is NonGenericType -> EagerStatic(it)
                is DynamicAppliedType -> EagerDynamic(it)
            }
        }
)

fun Applicable.forceStaticApply(typeArgs: List<NonGenericType>): StaticAppliedType
        = staticApply(typeArgs) ?: throw TypeApplicationException("Failed to static apply type args $typeArgs to $this")

fun Applicable.forceStaticApply(vararg typeArgs: NonGenericType): StaticAppliedType
        = forceStaticApply(typeArgs.toList())

fun Applicable.forceDynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType
        = dynamicApply(typeArgs) ?: throw TypeApplicationException("Failed to dynamically apply type args $typeArgs to $this")

fun Applicable.forceDynamicApply(vararg typeArgs: ApplicationParameter): DynamicAppliedType
        = forceDynamicApply(typeArgs.toList())

fun Applicable.forceApply(typeArgs: List<ApplicationParameter>): Type
        = apply(typeArgs) ?: throw TypeApplicationException("Failed to apply type args $typeArgs to $this")
