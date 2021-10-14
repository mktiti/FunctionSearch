package com.mktiti.fsearch.model.build.intermediate

import com.mktiti.fsearch.core.util.genericString
import kotlinx.serialization.Serializable

@Serializable
sealed class SemiInfo<S : SamInfo<*>> {

    abstract val info: IntMinInfo
    abstract val directSupers: List<IntMinInfo>
    abstract val satSupers: List<IntStaticCmi>
    abstract val samType: S?

    val nonGenericSuperCount: Int
        get() = directSupers.size + satSupers.size

    @Serializable
    data class DirectInfo(
            override val info: IntMinInfo,
            override val directSupers: List<IntMinInfo>,
            override val satSupers: List<IntStaticCmi>,
            override val samType: SamInfo.Direct?
    ) : SemiInfo<SamInfo.Direct>() {

        override fun toString(): String = buildString {
            append(info)
            append(" extends ")
            append((directSupers + satSupers).joinToString())
        }

    }

    @Serializable
    data class TemplateInfo(
            override val info: IntMinInfo,
            val typeParams: List<TemplateTypeParamInfo>,
            override val directSupers: List<IntMinInfo>,
            override val satSupers: List<IntStaticCmi>,
            override val samType: SamInfo.Generic?,
            val datSupers: Collection<DatInfo>
    ) : SemiInfo<SamInfo.Generic>() {

        override fun toString(): String = buildString {
            append(info)
            append(typeParams.genericString())
            append(" extends ")
            append((directSupers + satSupers + datSupers).joinToString())
        }

    }

}