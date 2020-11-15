package com.mktiti.fsearch.modules.javadoc

import java.net.URI

data class FunctionDoc(
        val link: URI? = null,
        val paramNames: List<String>? = null,
        val shortInfo: String? = null,
        val longInfo: String? = null
)
