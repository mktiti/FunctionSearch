@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.genericString
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class TypeInfoResult(
        val directInfos: Collection<SemiInfo.DirectInfo>,
        val templateInfos: Collection<SemiInfo.TemplateInfo>
)

@Serializable
data class FunctionInfoResult(
        val staticFunctions: Collection<RawFunInfo>,
        val instanceMethods: Map<MinimalInfo, Collection<RawFunInfo>>
)

@Serializable
data class ArtifactInfoResult(
        val typeInfo: TypeInfoResult,
        val funInfo: FunctionInfoResult
)

@Serializable
sealed class TypeParamInfo {
    @Serializable
    object Wildcard : TypeParamInfo() {
        override fun toString() = "*"
    }
    @Serializable
    object SelfRef : TypeParamInfo() {
        override fun toString() = "\$SELF"
    }
    @Serializable
    sealed class BoundedWildcard : TypeParamInfo() {

        abstract val bound: TypeParamInfo

        @Serializable
        class UpperWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? extends $bound"
        }
        @Serializable
        class LowerWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? super $bound"
        }
    }
    @Serializable
    class Direct(val arg: MinimalInfo) : TypeParamInfo() {
        override fun toString() = arg.toString()
    }
    @Serializable
    class Sat(val sat: CompleteMinInfo.Static) : TypeParamInfo() {
        override fun toString() = sat.toString()
    }
    @Serializable
    class Dat(val dat: DatInfo) : TypeParamInfo()  {
        override fun toString() = dat.toString()
    }
    @Serializable
    class Param(val param: Int) : TypeParamInfo() {
        override fun toString() = "\$$param"
    }
}

@Serializable
data class DatInfo(
        val template: MinimalInfo,
        val args: List<TypeParamInfo>
) {
    override fun toString(): String {
        return template.toString() + args.genericString()
    }
}

@Serializable
data class TemplateTypeParamInfo(
        val sign: String,
        val bounds: List<TypeParamInfo>
) {
    override fun toString(): String {
        return sign + if (bounds.isEmpty()) {
            ""
        } else {
            bounds.joinToString(prefix = " extends ", separator = ", ")
        }
    }
}

@Serializable
sealed class SamInfo<S : FunSignatureInfo<*>> {

    abstract val explicit: Boolean
    abstract val signature: S

    @Serializable
    class Direct(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Direct
    ) : SamInfo<FunSignatureInfo.Direct>()

    @Serializable
    class Generic(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Generic
    ) : SamInfo<FunSignatureInfo.Generic>()

}

@Serializable
sealed class SemiInfo<S : SamInfo<*>> {

    abstract val info: MinimalInfo
    abstract val directSupers: Collection<MinimalInfo>
    abstract val satSupers: Collection<CompleteMinInfo.Static>
    abstract val samType: S?

    val nonGenericSuperCount: Int
        get() = directSupers.size + satSupers.size

    @Serializable
    class DirectInfo(
            override val info: MinimalInfo,
            override val directSupers: Collection<MinimalInfo>,
            override val satSupers: Collection<CompleteMinInfo.Static>,
            override val samType: SamInfo.Direct?
    ) : SemiInfo<SamInfo.Direct>() {

        override fun toString(): String = buildString {
            append(info)
            append(" extends ")
            append((directSupers + satSupers).joinToString())
        }

    }

    @Serializable
    class TemplateInfo(
            override val info: MinimalInfo,
            val typeParams: List<TemplateTypeParamInfo>,
            override val directSupers: Collection<MinimalInfo>,
            override val satSupers: Collection<CompleteMinInfo.Static>,
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

@Serializable
sealed class RawFunInfo {

    abstract val info: FunctionInfo
    abstract val signature: FunSignatureInfo<*>

    companion object {
        fun of(info: FunctionInfo, signature: FunSignatureInfo<*>): RawFunInfo = when (signature) {
            is FunSignatureInfo.Direct -> Direct(info, signature)
            is FunSignatureInfo.Generic -> Generic(info, signature)
        }
    }

    @Serializable
    data class Direct(
            override val info: FunctionInfo,
            override val signature: FunSignatureInfo.Direct
    ) : RawFunInfo()

    @Serializable
    data class Generic(
            override val info: FunctionInfo,
            override val signature: FunSignatureInfo.Generic
    ) : RawFunInfo()

}

@Serializable
sealed class FunSignatureInfo<P> {

    abstract val inputs: List<Pair<String, P>>
    abstract val output: P
    abstract val typeParams: List<TemplateTypeParamInfo>

    @Serializable
    data class Direct(
            override val inputs: List<Pair<String, CompleteMinInfo.Static>>,
            override val output: CompleteMinInfo.Static
    ) : FunSignatureInfo<CompleteMinInfo.Static>() {

        override val typeParams: List<TemplateTypeParamInfo>
            get() = emptyList()

    }

    @Serializable
    data class Generic(
            override val typeParams: List<TemplateTypeParamInfo>,
            override val inputs: List<Pair<String, TypeParamInfo>>,
            override val output: TypeParamInfo
    ) : FunSignatureInfo<TypeParamInfo>()

}