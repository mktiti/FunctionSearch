package com.mktiti.fsearch.parser.connect.type

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.intermediate.DatInfo

interface SemiCreator<S : SemiType> {
    val unfinishedType: S
    val directSupers: MutableList<MinimalInfo>
    val satSupers: MutableList<CompleteMinInfo.Static>
    val nonGenericAppend: (TypeHolder.Static) -> Unit
    val done: Boolean
}

data class DirectCreator(
        override val unfinishedType: DirectType,
        override val directSupers: MutableList<MinimalInfo>,
        override val satSupers: MutableList<CompleteMinInfo.Static>,
        override val nonGenericAppend: (TypeHolder.Static) -> Unit
) : SemiCreator<DirectType> {

    override val done: Boolean
        get() = directSupers.isEmpty() && satSupers.isEmpty()

}

data class TemplateCreator(
        override val unfinishedType: TypeTemplate,
        override val directSupers: MutableList<MinimalInfo>,
        override val satSupers: MutableList<CompleteMinInfo.Static>,
        val datSupers: MutableList<DatInfo>,
        override val nonGenericAppend: (TypeHolder.Static) -> Unit,
        val datSuperAppend: (TypeHolder.Dynamic) -> Unit
) : SemiCreator<TypeTemplate>  {

    override val done: Boolean
        get() = directSupers.isEmpty() && satSupers.isEmpty() && datSupers.isEmpty()

    constructor(
            unfinishedType: TypeTemplate,
            mutableSupers: MutableList<TypeHolder<*, *>>,
            directSupers: MutableList<MinimalInfo>,
            satSupers: MutableList<CompleteMinInfo.Static>,
            datSupers: MutableList<DatInfo>
    ) : this(unfinishedType, directSupers, satSupers, datSupers, mutableSupers::add, mutableSupers::add)

}

sealed class HasUpdated {

    abstract val hadUpdate: Boolean

    fun update(): HasUpdated = Updated

    operator fun plus(updated: Boolean): HasUpdated = if (updated) update() else this

    object None : HasUpdated() {
        override val hadUpdate: Boolean
            get() = false
    }

    object Updated : HasUpdated() {
        override val hadUpdate: Boolean
            get() = true
    }
}