package com.mktiti.fsearch.util

fun <T> const(value: T): () -> T = { value }

fun <T> Boolean.map(onTrue: T, onFalse: T): T = if (this) onTrue else onFalse

fun <T> Boolean.map(onTrue: () -> T, onFalse: () -> T): T = if (this) onTrue() else onFalse()

fun <T : Any> T?.orElse(onNull: () -> T): T = this ?: onNull()
