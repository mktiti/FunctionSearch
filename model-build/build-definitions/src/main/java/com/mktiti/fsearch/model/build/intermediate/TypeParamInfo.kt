package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

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
        data class UpperWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? extends $bound"
        }
        @Serializable
        data class LowerWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? super $bound"
        }
    }
    @Serializable
    data class Direct(val arg: IntMinInfo) : TypeParamInfo() {
        override fun toString() = arg.toString()
    }
    @Serializable
    data class Sat(val sat: IntStaticCmi) : TypeParamInfo() {
        override fun toString() = sat.toString()
    }
    @Serializable
    data class Dat(val dat: DatInfo) : TypeParamInfo()  {
        override fun toString() = dat.toString()
    }
    @Serializable
    data class Param(val param: Int) : TypeParamInfo() {
        override fun toString() = "\$$param"
    }
}