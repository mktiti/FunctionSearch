package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

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