package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.parser.generated.QueryBaseVisitor
import com.mktiti.fsearch.parser.generated.QueryParser

object QueryTypeParameterSelector : QueryBaseVisitor<List<String>>() {

    override fun visitCompleteName(ctx: QueryParser.CompleteNameContext): List<String> {
        val signature = ctx.templateSignature()
        val name = ctx.fullName().text
        return if (signature != null) {
            visitTemplateSignature(signature)
        } else if (name.length == 1 && name.first().isLowerCase()) {
            listOf(name)
        } else {
            emptyList()
        }
    }

    override fun defaultResult(): List<String> = emptyList()

    override fun aggregateResult(aggregate: List<String>?, nextResult: List<String>): List<String> {
        return if (aggregate == null) {
            nextResult
        } else {
            (aggregate + nextResult).distinct()
        }
    }

}