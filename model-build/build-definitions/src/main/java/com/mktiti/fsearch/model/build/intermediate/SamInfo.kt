package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

@Serializable
sealed class SamInfo<S : FunSignatureInfo<*>> {

    abstract val explicit: Boolean
    abstract val signature: S

    @Serializable
    data class Direct(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Direct
    ) : SamInfo<FunSignatureInfo.Direct>()

    @Serializable
    data class Generic(
            override val explicit: Boolean,
            override val signature: FunSignatureInfo.Generic
    ) : SamInfo<FunSignatureInfo.Generic>()

}