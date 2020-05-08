package type

import MutablePrefixTree
import SemiType
import TypeTemplate
/*
object JavaSpecials {
    private val baseInfo = MinimalInfo(listOf("\$lang"), "")

    val javaArrayInfo = baseInfo.copy(simpleName = "\$array")
    val javaPrimitiveMap = mapOf(
            PrimitiveType.BOOL to baseInfo.copy(simpleName = "\$bool"),
            PrimitiveType.BYTE to baseInfo.copy(simpleName = "\$byte"),
            PrimitiveType.CHAR to baseInfo.copy(simpleName = "\$char"),
            PrimitiveType.SHORT to baseInfo.copy(simpleName = "\$short"),
            PrimitiveType.INT to baseInfo.copy(simpleName = "\$int"),
            PrimitiveType.LONG to baseInfo.copy(simpleName = "\$long"),
            PrimitiveType.FLOAT to baseInfo.copy(simpleName = "\$float"),
            PrimitiveType.DOUBLE to baseInfo.copy(simpleName = "\$double")
    )
}
 */

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
