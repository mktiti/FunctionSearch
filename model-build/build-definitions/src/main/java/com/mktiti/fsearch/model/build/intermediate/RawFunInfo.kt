package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

@Serializable
sealed class RawFunInfo {

    abstract val info: IntFunInfo
    abstract val signature: FunSignatureInfo<*>

    companion object {
        fun of(info: IntFunInfo, signature: FunSignatureInfo<*>): RawFunInfo = when (signature) {
            is FunSignatureInfo.Direct -> Direct(info, signature)
            is FunSignatureInfo.Generic -> Generic(info, signature)
        }
    }

    @Serializable
    data class Direct(
            override val info: IntFunInfo,
            override val signature: FunSignatureInfo.Direct
    ) : RawFunInfo()

    @Serializable
    data class Generic(
            override val info: IntFunInfo,
            override val signature: FunSignatureInfo.Generic
    ) : RawFunInfo()

}