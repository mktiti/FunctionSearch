package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.parser.generated.QueryBaseVisitor
import com.mktiti.fsearch.parser.generated.QueryParser

object QueryTypeParameterSelector : QueryBaseVisitor<Set<String>>() {

    override fun visitCompleteName(ctx: QueryParser.CompleteNameContext): Set<String> {
        val signature = ctx.templateSignature()
        val name = ctx.fullName()?.text ?: return emptySet()
        return if (signature != null) {
            visitTemplateSignature(signature)
        } else if (name.length == 1 && name.first().isLowerCase()) {
            setOf(name)
        } else {
            emptySet()
        }
    }

    override fun defaultResult(): Set<String> = emptySet()

    override fun aggregateResult(aggregate: Set<String>?, nextResult: Set<String>): Set<String> {
        return if (aggregate == null) {
            nextResult
        } else {
            aggregate + nextResult
        }
    }

}