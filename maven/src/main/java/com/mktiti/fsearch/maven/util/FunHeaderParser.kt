package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.orElse

object FunHeaderParser {

    data class HeaderData(
            val name: String,
            val paramNames: List<String>?,
            val paramTypes: List<FunIdParam>
    )

    fun parseParam(infoRepo: JavaInfoRepo, param: String): FunIdParam {
        fun String.parseSig(): FunIdParam = when {
            endsWith("...") -> FunIdParam.Array(removeSuffix("...").parseSig())
            endsWith("[]") -> FunIdParam.Array(removeSuffix("[]").parseSig())
            else -> parseTrimmedParam(infoRepo, this)
        }

        return param.trim().parseSig()
    }

    fun parseTrimmedParam(infoRepo: JavaInfoRepo, param: String): FunIdParam {
        return PrimitiveType.fromNameSafe(param)?.let {
            FunIdParam.Type(infoRepo.primitive(it))
        }.orElse {
            val parsed: FunIdParam = if ('.' !in param && param.all { it.isDigit() || it.isUpperCase() }) {
                FunIdParam.TypeParam(param)
            } else {
                val (pack, type) = param.split('.').cutLast()
                FunIdParam.Type(MinimalInfo(pack, type))
            }

            parsed
        }
    }

    fun parseFunHeader(infoRepo: JavaInfoRepo, value: String): HeaderData? {
        val parts = value.split('(')
        val name = parts.getOrNull(0) ?: return null
        val inParen = parts.getOrNull(1)?.dropLast(1) ?: return null

        if (inParen.isBlank()) {
            return HeaderData(name, emptyList(), emptyList())
        }

        val withoutAnnotations = inParen.split("\\s+".toRegex()).filterNot {
            it.startsWith("@")
        }.joinToString(prefix = "", separator = " ", postfix = "")

        data class NestAcc(
                val nest: Int = 0,
                val topLevel: List<Char> = emptyList()
        ) {
            fun onOpen() = copy(nest = nest + 1)

            fun onClose() = copy(nest = nest - 1)

            fun onOther(c: Char) = if (nest == 0) copy(topLevel = topLevel + c) else this
        }

        val topLevel = withoutAnnotations.toCharArray().fold(NestAcc()) { acc, c ->
            when (c) {
                '<' -> acc.onOpen()
                '>' -> acc.onClose()
                else -> acc.onOther(c)
            }
        }.let { String(it.topLevel.toCharArray()) }

        val (names, params) = topLevel.split(',').map {
            val paramParts = it.trim().split("\\W+".toRegex(), limit = 2)

            val paramName = paramParts.getOrNull(1) ?: return null
            val type = parseParam(infoRepo, paramParts.first())
            paramName to type
        }.unzip()

        return HeaderData(
                name = name,
                paramNames = names.liftNull(),
                paramTypes = params
        )
    }

}