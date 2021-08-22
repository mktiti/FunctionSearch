package com.mktiti.fsearch.parser.type
/*
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

sealed class SamTypeCreator<C>(
        val explicit: Boolean,
        val inputs: List<C>,
        val output: C
) {

    class Direct(
            explicit: Boolean,
            inputs: List<MinimalInfo>,
            output: MinimalInfo
    ) : SamTypeCreator<MinimalInfo>(explicit, inputs, output)

    class Dat(
            explicit: Boolean,
            inputs: List<DatCreator>,
            output: DatCreator
    ) : SamTypeCreator<DatCreator>(explicit, inputs, output)

}

sealed class TypeCreator<T : SemiType, S : SamTypeCreator<*>>(
        val unfinishedType: T,
        val addNonGenericSuper: (Type.NonGenericType) -> Unit,
        val directSupers: List<MinimalInfo>,
        val templateSupers: MutableList<DatCreator>,
        val samTypeCreator: S?
)

class DirectCreator(
        unfinishedType: Type.NonGenericType.DirectType,
        addNonGenericSuper: (Type.NonGenericType) -> Unit,
        directSupers: List<MinimalInfo>,
        templateSupers: MutableList<DatCreator>,
        samTypeCreator: SamTypeCreator.Direct?
) : TypeCreator<Type.NonGenericType.DirectType, SamTypeCreator.Direct>(unfinishedType, addNonGenericSuper, directSupers, templateSupers, samTypeCreator)

class TemplateCreator(
        unfinishedType: TypeTemplate,
        directSuperAppender: (Type.NonGenericType) -> Unit,
        directSupers: List<MinimalInfo>,
        templateSupers: MutableList<DatCreator>,
        samTypeCreator: SamTypeCreator.Dat?,
        val templateSuperAppender: (Type.DynamicAppliedType) -> Unit
) : TypeCreator<TypeTemplate, SamTypeCreator.Dat>(unfinishedType, directSuperAppender, directSupers, templateSupers, samTypeCreator)


 */