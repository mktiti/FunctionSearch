package query

import QueryBaseVisitor
import QueryParser

object QueryTypeParameterSelector : QueryBaseVisitor<List<String>>() {

    override fun visitTemplateArg(ctx: QueryParser.TemplateArgContext): List<String> {
        return ctx.TEMPLATE_PARAM()?.let { listOf(it.text) } ?: visitCompleteName(ctx.completeName())
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