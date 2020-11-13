package com.mktiti.fsearch.util

inline fun <reified S> Collection<*>.castIfAllInstance(): List<S>? = map {
    if (it is S) it else return null
}

inline fun <K, V, reified S> Map<K, V>.castIfAllValuesInstance(): Map<K, S>? = mapValues {
    if (it is S) {
        it
    } else {
        return null
    }
}