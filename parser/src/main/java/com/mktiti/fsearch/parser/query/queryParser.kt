package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.parser.generated.QueryLexer
import com.mktiti.fsearch.parser.generated.QueryParser.*
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode

typealias VirtParamTable = Map<String, NonGenericType>

interface QueryParser {

    data class ParseResult(
            val query: QueryType,
            val virtualTypes: List<DirectType>
    )

    fun parse(query: String): ParseResult
}

class AntlrQueryParser(
        private val javaRepo: JavaRepo,
        private val infoRepo: JavaInfoRepo,
        private val typeResolver: TypeResolver
) : QueryParser {

    private fun ifArray(type: NonGenericType, arrayLiteral: List<TerminalNode>?): NonGenericType {
        return buildArrayOf(type, arrayLiteral?.size ?: 0)
    }

    private tailrec fun buildArrayOf(type: NonGenericType, depth: Int): NonGenericType = if (depth == 0) {
        type
    } else {
        buildArrayOf(javaRepo.arrayOf(type.holder()), depth - 1)
    }

    private fun buildFullType(completeName: CompleteNameContext, paramVirtualTypes: VirtParamTable): NonGenericType {
        val name = completeName.fullName().text

        paramVirtualTypes[name]?.let { virtual ->
            return ifArray(virtual, completeName.ARRAY_LITERAL())
        }

        val type: NonGenericType = when (val typeSignature = completeName.templateSignature()) {
            null -> {
                PrimitiveType.fromNameSafe(name)?.let(javaRepo::primitive)?.with(typeResolver)
                        ?: typeResolver.get(name, allowSimple = true)
                        ?: throw TypeException("Simple type $name not found")
            }
            else -> {
                val typeArgs = typeSignature.completeName().map { buildFullType(it, paramVirtualTypes) }
                typeResolver.template(name, allowSimple = true, paramCount = typeArgs.size)
                        ?.forceStaticApply(TypeHolder.staticDirects(typeArgs))
                        ?: throw TypeException("Generic type $name not found")
            }
        }

        return ifArray(type, completeName.ARRAY_LITERAL())
    }

    private fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable): NonGenericType {
        val query = buildFunSignature(funCtx, paramVirtualTypes)
        return QueryType.functionType(query.inputParameters, query.output, infoRepo)
    }

    private tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable): NonGenericType {
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

    private fun buildVirtual(context: VirtualDeclarationContext, virtualMap: Map<String, NonGenericType>, defaultRoot: TypeHolder.Static): Pair<String, DirectType> {
        val name = context.SIMPLE_NAME().text

        return name to when (val boundNames = context.declarationBounds()?.completeName()) {
            null -> {
                QueryType.virtualType(name, listOf(defaultRoot))
            }
            else -> {
                val mutBounds: MutableList<TypeHolder.Static> = ArrayList(boundNames.size)
                QueryType.virtualType(name, mutBounds).also {
                    val updatedVirtMap = virtualMap + (name to it)
                    boundNames.map { boundName ->
                        mutBounds += buildFullType(boundName, updatedVirtMap).holder()
                    }
                }
            }
        }
    }

    private fun buildVirtuals(context: QueryContext, names: Set<String>, defaultRoot: TypeHolder.Static): Map<String, DirectType> {
        val explicitDeclarations = context.virtualDeclarations()?.virtualDeclaration() ?: emptyList()
        val explicitNames = explicitDeclarations.mapNotNull { it.SIMPLE_NAME().text }

        val implicits = (names - explicitNames).map { name ->
            name to QueryType.virtualType(name, listOf(defaultRoot))
        }.toMap()

        val explicits = explicitDeclarations.fold(implicits) { alreadyDone, toCreate ->
            val created = buildVirtual(toCreate, alreadyDone, defaultRoot)
            implicits + created
        }

        return implicits + explicits
    }

    @Throws(TypeException::class)
    override fun parse(query: String): QueryParser.ParseResult {
        val lexer = QueryLexer(CharStreams.fromString(query))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = com.mktiti.fsearch.parser.generated.QueryParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val parseTree: QueryContext = parser.query()

        val typeParams = QueryTypeParameterSelector.visit(parseTree)
        val root = javaRepo.objectType
        val paramVirtualTypes = buildVirtuals(parseTree, typeParams, root)

        val queryType = buildFunSignature(parseTree.funSignature(), paramVirtualTypes)
        return QueryParser.ParseResult(queryType, paramVirtualTypes.map { it.value })
    }

}
