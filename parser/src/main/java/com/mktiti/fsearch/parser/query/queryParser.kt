package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.parser.generated.QueryLexer
import com.mktiti.fsearch.parser.generated.QueryParser.*
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode

typealias VirtParamTable = Map<String, Type.NonGenericType>

interface QueryParser {
    fun parse(query: String): QueryType
}

class AntlrQueryParser(
        private val javaRepo: JavaRepo,
        private val typeResolver: TypeResolver
) : QueryParser {

    private fun ifArray(type: Type.NonGenericType, arrayLiteral: List<TerminalNode>?): Type.NonGenericType {
        return arrayOf(type, arrayLiteral?.size ?: 0)
    }

    private tailrec fun arrayOf(type: Type.NonGenericType, depth: Int): Type.NonGenericType = if (depth == 0) {
        type
    } else {
        arrayOf(javaRepo.arrayOf(type.holder()), depth - 1)
    }

    private fun buildFullType(completeName: CompleteNameContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val name = completeName.fullName().text

        paramVirtualTypes[name]?.let { virtual ->
            return ifArray(virtual, completeName.ARRAY_LITERAL())
        }

        val type: Type.NonGenericType = when (val typeSignature = completeName.templateSignature()) {
            null -> {
                PrimitiveType.fromNameSafe(name)?.let(javaRepo::primitive)?.with(typeResolver)
                        ?: typeResolver.get(name, allowSimple = true)
                        ?: throw TypeException("Simple type $name not found")
            }
            else -> {
                val typeArgs = typeSignature.completeName().map { buildFullType(it, paramVirtualTypes) }
                typeResolver.template(name, allowSimple = true)?.forceStaticApply(TypeHolder.staticDirects(typeArgs))
                        ?: throw TypeException("Generic type $name not found")
            }
        }

        return ifArray(type, completeName.ARRAY_LITERAL())
    }

    private fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val query = buildFunSignature(funCtx, paramVirtualTypes)
        return QueryType.functionType(query.inputParameters, query.output)
    }

    private tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val nested = par.funArg()

        return when {
            nested.completeName() != null -> buildFullType(nested.completeName(), paramVirtualTypes)
            nested.funSignature() != null -> ifArray(buildFunArg(nested.funSignature(), paramVirtualTypes), nested.ARRAY_LITERAL())
            else -> buildArg(par.wrappedFunArg(), paramVirtualTypes)
        }
    }

    private fun buildFunSignature(
            funSignature: FunSignatureContext,
            paramVirtualTypes: VirtParamTable
    ): QueryType {
        fun WrappedFunArgContext.mapArg() = buildArg(this, paramVirtualTypes)

        val inArgs = (funSignature.inArgs().wrappedFunArg() ?: emptyList()).map { it.mapArg() }
        val outArg = funSignature.outArg().wrappedFunArg()?.mapArg() ?: javaRepo.voidType.with(typeResolver) ?: error("Void not found")

        return QueryType(inArgs, outArg)
    }

    @Throws(TypeException::class)
    override fun parse(query: String): QueryType {
        val lexer = QueryLexer(CharStreams.fromString(query))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = com.mktiti.fsearch.parser.generated.QueryParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val parseTree: QueryContext = parser.query()

        val typeParams = QueryTypeParameterSelector.visit(parseTree)
        val root = javaRepo.objectType.with(typeResolver) ?: error("Object not found") // TODO
        val paramVirtualTypes = typeParams.map { param ->
            param to QueryType.virtualType(param, listOf(root))
        }.toMap()

        return buildFunSignature(parseTree.funSignature(), paramVirtualTypes)
    }

}
