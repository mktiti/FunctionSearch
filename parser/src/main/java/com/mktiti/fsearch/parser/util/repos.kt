package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.type.MinimalInfo

fun <T, R : Any> Collection<T>.fromAny(getter: T.() -> R?): R?
        = asSequence().mapNotNull { it.getter() }.firstOrNull()

fun Collection<TypeRepo>.anyDirect(name: String, allowSimple: Boolean = false) = fromAny { get(name, allowSimple) }

fun Collection<TypeRepo>.anyDirect(info: MinimalInfo) = fromAny { get(info) }

fun Collection<TypeRepo>.anyTemplate(name: String, allowSimple: Boolean = false) = fromAny { template(name, allowSimple) }

fun Collection<TypeRepo>.anyTemplate(info: MinimalInfo) = fromAny { template(info) }
