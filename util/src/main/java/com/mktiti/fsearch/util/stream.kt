package com.mktiti.fsearch.util

import java.util.stream.Stream

fun <T, R> Stream<T>.mapNotNull(mapper: (T) -> R?): Stream<R> = map { mapper(it) }.filter { it != null }.map { it }