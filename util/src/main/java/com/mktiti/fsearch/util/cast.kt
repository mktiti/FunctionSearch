package com.mktiti.fsearch.util

inline fun <reified S> Collection<*>.castIfAllInstance(): List<S>? = map {
    if (it is S) it else return null
}