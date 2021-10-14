package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

@Serializable
data class DatInfo(
        val template: IntMinInfo,
        val args: List<TypeParamInfo>
)