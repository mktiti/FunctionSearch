package com.mktiti.fsearch.model.build.intermediate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
sealed class SamInfo<S : FunSignatureInfo<*>> {

    abstract val explicit: Boolean
    abstract val signature: S

        data class Direct(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Direct
    ) : SamInfo<FunSignatureInfo.Direct>()

        data class Generic(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Generic
    ) : SamInfo<FunSignatureInfo.Generic>()

}