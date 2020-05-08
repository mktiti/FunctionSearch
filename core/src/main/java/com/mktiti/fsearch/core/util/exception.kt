package com.mktiti.fsearch.core.util

open class TypeException(message: String) : RuntimeException(message)

class TypeApplicationException(message: String) : TypeException(message)

class TypeInfoParseException(parsed: String, message: String) : TypeException("Failed to parse $parsed into TypeInfo - $message")
