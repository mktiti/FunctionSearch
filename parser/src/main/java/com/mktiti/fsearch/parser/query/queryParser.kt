package com.mktiti.fsearch.parser.query

import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.InfoHelper
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

    fun parse(query: String, imports: QueryImports): ParseResult
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

    private fun <T> getTypeBase(
            name: String,
            imports: QueryImports,
            getter: TypeResolver.(info: MinimalInfo) -> T?,
            simpleGetter: TypeResolver.(name: String) -> T?
    ): T {
        val info = InfoHelper.minimalInfo(name) ?: throw TypeException("Minimal info '$name' invalid")
        return if (info.packageName.isEmpty()) {
            val simpleName = info.simpleName

            imports.potentialInfos(simpleName).mapNotNull {
                typeResolver.getter(it)
            }.firstOrNull()?.let {
                return it
            }

            typeResolver.simpleGetter(simpleName) ?: throw TypeException("Type $simpleName not found")
        } else {
            typeResolver.getter(info) ?: throw TypeException("Type $name not found")
        }
    }

    private fun buildNonGeneric(name: String, imports: QueryImports): NonGenericType {
        PrimitiveType.fromNameSafe(name)?.let {
            return javaRepo.primitive(it).with(typeResolver)
        }

        return getTypeBase(name, imports, getter = TypeResolver::get) {
            typeResolver.get(it, allowSimple = true)
        }
    }

    private fun buildGeneric(
            name: String,
            templateContext: TemplateSignatureContext,
            paramVirtualTypes: VirtParamTable,
            imports: QueryImports
    ): NonGenericType {
        val template = getTypeBase(name, imports, getter = TypeResolver::template) {
            typeResolver.template(it, allowSimple = true)
        }

        val typeArgs = templateContext.completeName().map { buildFullType(it, paramVirtualTypes, imports) }
        return template.forceStaticApply(TypeHolder.staticDirects(typeArgs))
    }

    private fun buildFullType(
            completeName: CompleteNameContext,
            paramVirtualTypes: VirtParamTable,
            imports: QueryImports
    ): NonGenericType {
        val type: NonGenericType = if (completeName.WILDCARD() != null) {
            QueryType.wildcard(infoRepo)
        } else {
            val name = completeName.fullName().text
            val templateSignature = completeName.templateSignature()

            paramVirtualTypes[name]?.let { virtual ->
                if (templateSignature != null) {
                    throw TypeException("Type parameter '$name' cannot have type arguments")
                }
                return ifArray(virtual, completeName.ARRAY_LITERAL())
            }

            if (templateSignature == null) {
                buildNonGeneric(name, imports)
            } else {
                buildGeneric(name, templateSignature, paramVirtualTypes, imports)
            }
        }

        return ifArray(type, completeName.ARRAY_LITERAL())
    }

    private fun buildFunArg(
            funCtx: FunSignatureContext,
            paramVirtualTypes: VirtParamTable,
            imports: QueryImports
    ): NonGenericType {
        val query = buildFunSignature(funCtx, paramVirtualTypes, imports)
        return QueryType.functionType(query.inputParameters, query.output, infoRepo)
    }

    private tailrec fun buildArg(
            par: WrappedFunArgContext,
            paramVirtualTypes: VirtParamTable,
            imports: QueryImports
    ): NonGenericType {
        val nested = par.funArg()

        return when {
            nested.completeName() != null -> buildFullType(nested.completeName(), paramVirtualTypes, imports)
            nested.funSignature() != null -> ifArray(buildFunArg(nested.funSignature(), paramVirtualTypes, imports), nested.ARRAY_LITERAL())
            else -> buildArg(par.wrappedFunArg(), paramVirtualTypes, imports)
        }
    }

    private fun buildFunSignature(
            funSignature: FunSignatureContext,
            paramVirtualTypes: VirtParamTable,
            imports: QueryImports
    ): QueryType {
        fun WrappedFunArgContext.mapArg() = buildArg(this, paramVirtualTypes, imports)

        val inArgs = (funSignature.inArgs().wrappedFunArg() ?: emptyList()).map { it.mapArg() }
        val outArg = funSignature.outArg().wrappedFunArg()?.mapArg() ?: javaRepo.voidType.with(typeResolver) ?: error("Void not found")

        return QueryType(inArgs, outArg)
    }

    private fun buildVirtual(
            context: VirtualDeclarationContext,
            virtualMap: Map<String, NonGenericType>,
            defaultRoot: TypeHolder.Static,
            imports: QueryImports
    ): Pair<String, DirectType> {
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
                        mutBounds += buildFullType(boundName, updatedVirtMap, imports).holder()
                    }
                }
            }
        }
    }

    private fun buildVirtuals(
            context: QueryContext,
            names: Set<String>,
            defaultRoot: TypeHolder.Static,
            imports: QueryImports
    ): Map<String, DirectType> {
        val explicitDeclarations = context.virtualDeclarations()?.virtualDeclaration() ?: emptyList()
        val explicitNames = explicitDeclarations.mapNotNull { it.SIMPLE_NAME().text }

        val implicits = (names - explicitNames).map { name ->
            name to QueryType.virtualType(name, listOf(defaultRoot))
        }.toMap()

        val explicits = explicitDeclarations.fold(implicits) { alreadyDone, toCreate ->
            val created = buildVirtual(toCreate, alreadyDone, defaultRoot, imports)
            implicits + created
        }

        return implicits + explicits
    }

    @Throws(TypeException::class)
    override fun parse(query: String, imports: QueryImports): QueryParser.ParseResult {
        val lexer = QueryLexer(CharStreams.fromString(query))
        lexer.removeErrorListeners()
        lexer.addErrorListener(ExceptionErrorListener)

        val parser = com.mktiti.fsearch.parser.generated.QueryParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(ExceptionErrorListener)

        val parseTree: QueryContext = parser.query()

        val typeParams = QueryTypeParameterSelector.visit(parseTree)
        val root = javaRepo.objectType
        val paramVirtualTypes = buildVirtuals(parseTree, typeParams, root, imports)

        val queryType = buildFunSignature(parseTree.funSignature(), paramVirtualTypes, imports)
        return QueryParser.ParseResult(queryType, paramVirtualTypes.map { it.value })
    }

}
