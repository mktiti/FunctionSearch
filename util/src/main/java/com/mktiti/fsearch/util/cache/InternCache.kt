package com.mktiti.fsearch.util.cache

interface InternCache<T> {

    fun intern(value: T): T

    fun clean()

}