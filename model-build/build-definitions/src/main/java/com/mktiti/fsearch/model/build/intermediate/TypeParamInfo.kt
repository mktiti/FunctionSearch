package com.mktiti.fsearch.model.build.intermediate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
sealed class TypeParamInfo {
        object Wildcard : TypeParamInfo() {
        override fun toString() = "*"
    }
        object SelfRef : TypeParamInfo() {
        override fun toString() = "\$SELF"
    }
        @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    sealed class BoundedWildcard : TypeParamInfo() {

        abstract val bound: TypeParamInfo

                data class UpperWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? extends $bound"
        }
                data class LowerWildcard(override val bound: TypeParamInfo) : BoundedWildcard() {
            override fun toString() = "? super $bound"
        }
    }
        data class Direct(val arg: IntMinInfo) : TypeParamInfo() {
        override fun toString() = arg.toString()
    }
        data class Sat(val sat: IntStaticCmi) : TypeParamInfo() {
        override fun toString() = sat.toString()
    }
        data class Dat(val dat: DatInfo) : TypeParamInfo()  {
        override fun toString() = dat.toString()
    }
        data class Param(val param: Int) : TypeParamInfo() {
        override fun toString() = "\$$param"
    }
}