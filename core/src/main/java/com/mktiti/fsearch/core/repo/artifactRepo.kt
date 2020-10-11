package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*

interface ArtifactTypeRepo {

    val artefact: String // TODO
    val javaRepo: JavaRepo

    val allTypes: Collection<Type>
    val allTemplates: Collection<TypeTemplate>

    operator fun get(name: String, allowSimple: Boolean = false): Type.NonGenericType.DirectType?

    operator fun get(info: MinimalInfo): Type.NonGenericType.DirectType?

    fun template(name: String, allowSimple: Boolean = false): TypeTemplate?

    fun template(info: MinimalInfo): TypeTemplate?

    fun samType(type: SemiType): SamType<*>?

}
