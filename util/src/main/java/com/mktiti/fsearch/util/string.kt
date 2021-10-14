package com.mktiti.fsearch.util

import java.util.*

fun String.indexOfOrNull(sub: String, startIndex: Int = 0): Int? = when (val index = indexOf(sub, startIndex)) {
    -1 -> null
    else -> index
}

fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}