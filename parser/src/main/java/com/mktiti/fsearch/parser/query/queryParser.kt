package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.parser.generated.QueryLexer
import com.mktiti.fsearch.parser.generated.QueryParser.*
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.fit.virtualType
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import com.mktiti.fsearch.parser.util.anyDirect
import com.mktiti.fsearch.parser.util.anyTemplate
import com.mktiti.fsearch.parser.util.fromAny
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

typealias VirtParamTable = Map<String, Type.NonGenericType>

interface QueryParser {
    fun parse(query: String): QueryType
}

class AntlrQueryParser(
        private val javaRepo: JavaRepo,
        private val typeRepos: Collection<TypeRepo>
) : QueryParser {

    private fun buildFullType(completeName: CompleteNameContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val name = completeName.fullName().text

        paramVirtualTypes[name]?.let { virtual ->
            return virtual
        }

        return when (val typeSignature = completeName.templateSignature()) {
            null -> {
                PrimitiveType.fromNameSafe(name)?.let(javaRepo::primitive)
                        ?: typeRepos.anyDirect(name, allowSimple = true)
                        ?: throw TypeException("Simple type $name not found")
            }
            else -> {
                val typeArgs = typeSignature.completeName().map { buildFullType(it, paramVirtualTypes) }
                typeRepos.anyTemplate(name, allowSimple = true)?.forceStaticApply(typeArgs)
                        ?: throw TypeException("Generic type $name not found")
            }
        }
    }

    private fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val query = buildFunSignature(funCtx, paramVirtualTypes)
        val funTemplate = typeRepos.fromAny { functionType(query.inputParameters.size) }!! // TODO
        return funTemplate.forceStaticApply(query.allParams)
    }

    private tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable): Type.NonGenericType {
        val nested = par.funArg()

        return when {
            nested.completeName() != null -> buildFullType(nested.completeName(), paramVirtualTypes)
            nested.funSignature() != null -> buildFunArg(nested.funSignature(), paramVirtualTypes)
            else -> buildArg(par.wrappedFunArg(), paramVirtualTypes)
        }
    }

    private fun buildFunSignature(
            funSignature: FunSignatureContext,
            paramVirtualTypes: VirtParamTable
    ): QueryType {
        fun WrappedFunArgContext.mapArg() = buildArg(this, paramVirtualTypes)

        val inArgs = (funSignature.inArgs().wrappedFunArg() ?: emptyList()).map { it.mapArg() }
        val outArg = funSignature.outArg().wrappedFunArg()?.mapArg() ?: javaRepo.voidType

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
        val root = typeRepos.fromAny { rootType }!! // TODO
        val paramVirtualTypes = typeParams.map { param ->
            param to virtualType(param, listOf(root))
        }.toMap()

        return buildFunSignature(parseTree.funSignature(), paramVirtualTypes)
    }

}
