@file:Suppress("UNUSED")

package com.mktiti.fsearch.core.type

enum class PrimitiveType(val javaName: String, val signature: Char) {
    BOOL("boolean", 'Z'),
    BYTE("byte", 'B'),
    CHAR("char", 'C'),
    SHORT("short", 'S'),
    INT("int", 'I'),
    LONG("long", 'J'),
    FLOAT("float", 'F'),
    DOUBLE("double", 'D');

    companion object {
        fun fromNameSafe(name: String) = values().find { it.javaName == name }

        @Suppress("MemberVisibilityCanBePrivate")
        fun fromSignature(sign: Char) = values().find { it.signature == sign }!!

        fun fromSignature(sign: String) = fromSignature(sign.first())
    }

    override fun toString() = javaName

}