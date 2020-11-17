package com.mktiti.fsearch.core.javadoc

import java.net.URI

data class FunctionDoc(
        val link: URI? = null,
        val paramNames: List<String>? = null,
        val shortInfo: String? = null,
        val longInfo: String? = null
) {

    companion object {
        private fun mergeString(current: String?, other: String?): String? = when {
            current == null -> other
            current.isNotBlank() -> current
            other == null -> current
            else -> other
        }
    }

    fun mergeMissing(other: FunctionDoc): FunctionDoc = FunctionDoc(
            link = link ?: other.link,
            paramNames = paramNames ?: other.paramNames,
            shortInfo = mergeString(shortInfo, other.shortInfo),
            longInfo = mergeString(longInfo, other.longInfo)
    )

}
