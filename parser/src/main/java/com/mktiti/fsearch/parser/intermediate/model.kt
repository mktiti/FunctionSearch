package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.genericString

data class TypeInfoResult(
        val directInfos: Collection<SemiInfo.DirectInfo>,
        val templateInfos: Collection<SemiInfo.TemplateInfo>
)

sealed class TypeParamInfo {
    object Wildcard : TypeParamInfo() {
        override fun toString() = "*"
    }
    object SelfRef : TypeParamInfo() {
        override fun toString() = "\$SELF"
    }
    sealed class BoundedWildcard(val bound: TypeParamInfo) : TypeParamInfo() {
        class UpperWildcard(bound: TypeParamInfo) : BoundedWildcard(bound) {
            override fun toString() = "? extends $bound"
        }
        class LowerWildcard(bound: TypeParamInfo) : BoundedWildcard(bound) {
            override fun toString() = "? super $bound"
        }
    }
    class Direct(val arg: MinimalInfo) : TypeParamInfo() {
        override fun toString() = arg.toString()
    }
    class Sat(val sat: CompleteMinInfo.Static) : TypeParamInfo() {
        override fun toString() = sat.toString()
    }
    class Dat(val dat: DatInfo) : TypeParamInfo()  {
        override fun toString() = dat.toString()
    }
    class Param(val param: Int) : TypeParamInfo() {
        override fun toString() = "\$$param"
    }
}

data class DatInfo(
        val template: MinimalInfo,
        val args: List<TypeParamInfo>
) {
    override fun toString(): String {
        return template.toString() + args.genericString()
    }
}

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

sealed class SamInfo<S : FunSignatureInfo<*>>(
        val explicit: Boolean,
        val signature: S
) {

    class Direct(
            explicit: Boolean,
            signature: FunSignatureInfo.Direct
    ) : SamInfo<FunSignatureInfo.Direct>(explicit, signature)

    class Generic(
            explicit: Boolean,
            signature: FunSignatureInfo.Generic
    ) : SamInfo<FunSignatureInfo.Generic>(explicit, signature)

}

sealed class SemiInfo<S : SamInfo<*>>(
        val info: MinimalInfo,
        val directSupers: Collection<MinimalInfo>,
        val satSupers: Collection<CompleteMinInfo.Static>,
        val samType: S?
) {

    val nonGenericSuperCount: Int
        get() = directSupers.size + satSupers.size

    class DirectInfo(
            info: MinimalInfo,
            directSupers: Collection<MinimalInfo>,
            satSupers: Collection<CompleteMinInfo.Static>,
            samInfo: SamInfo.Direct?
    ) : SemiInfo<SamInfo.Direct>(info, directSupers, satSupers, samInfo) {

        override fun toString(): String = buildString {
            append(info)
            append(" extends ")
            append((directSupers + satSupers).joinToString())
        }

    }

    class TemplateInfo(
            info: MinimalInfo,
            val typeParams: List<TemplateTypeParamInfo>,
            directSupers: Collection<MinimalInfo>,
            satSupers: Collection<CompleteMinInfo.Static>,
            samInfo: SamInfo.Generic?,
            val datSupers: Collection<DatInfo>
    ) : SemiInfo<SamInfo.Generic>(info, directSupers, satSupers, samInfo) {

        override fun toString(): String = buildString {
            append(info)
            append(typeParams.genericString())
            append(" extends ")
            append((directSupers + satSupers + datSupers).joinToString())
        }

    }

}

data class RawFunInfo<I : FunSignatureInfo<*>>(
        val info: FunctionInfo,
        val signature: I
)

sealed class FunSignatureInfo<P>(
        val inputs: List<Pair<String, P>>,
        val output: P
) {

    abstract val typeParams: List<TemplateTypeParamInfo>

    class Direct(
            inputs: List<Pair<String, CompleteMinInfo.Static>>,
            output: CompleteMinInfo.Static
    ) : FunSignatureInfo<CompleteMinInfo.Static>(inputs, output) {

        override val typeParams: List<TemplateTypeParamInfo>
            get() = emptyList()

    }

    class Generic(
            override val typeParams: List<TemplateTypeParamInfo>,
            inputs: List<Pair<String, TypeParamInfo>>,
            output: TypeParamInfo
    ) : FunSignatureInfo<TypeParamInfo>(inputs, output)

}