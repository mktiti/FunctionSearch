package com.mktiti.fsearch.util

fun String.indexOfOrNull(sub: String, startIndex: Int = 0): Int? = when (val index = indexOf(sub, startIndex)) {
    -1 -> null
    else -> index
}