package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeTemplate

sealed class TypeArgCreator {
    object Wildcard : TypeArgCreator()
    class UpperWildcard(val bound: TypeArgCreator) : TypeArgCreator()
    class Direct(val arg: MinimalInfo) : TypeArgCreator()
    class Dat(val dat: DatCreator) : TypeArgCreator()
    class Param(val sign: Int) : TypeArgCreator()
}

data class DatCreator(
        val template: MinimalInfo,
        val args: List<TypeArgCreator>
)

sealed class TypeCreator<T : SemiType>(
        val unfinishedType: T,
        val addNonGenericSuper: (Type.NonGenericType) -> Unit,
        val directSupers: List<MinimalInfo>,
        val templateSupers: MutableList<DatCreator>
)

class DirectCreator(
        unfinishedType: Type.NonGenericType.DirectType,
        addNonGenericSuper: (Type.NonGenericType) -> Unit,
        directSupers: List<MinimalInfo>,
        templateSupers: MutableList<DatCreator>
) : TypeCreator<Type.NonGenericType.DirectType>(unfinishedType, addNonGenericSuper, directSupers, templateSupers)

class TemplateCreator(
        unfinishedType: TypeTemplate,
        directSuperAppender: (Type.NonGenericType) -> Unit,
        directSupers: List<MinimalInfo>,
        templateSupers: MutableList<DatCreator>,
        val templateSuperAppender: (Type.DynamicAppliedType) -> Unit
) : TypeCreator<TypeTemplate>(unfinishedType, directSuperAppender, directSupers, templateSupers)
