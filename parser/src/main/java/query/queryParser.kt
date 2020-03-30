package query

import ApplicationParameter.Substitution.ParamSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.Wildcard.BoundedWildcard.LowerBound
import ApplicationParameter.Wildcard.BoundedWildcard.UpperBound
import FunctionInfo
import FunctionObj
import QueryLexer
import QueryParser
import QueryParser.*
import QueryType
import Type
import TypeRepo
import TypeSignature
import createTestRepo
import cutLast
import forceDynamicApply
import forceStaticApply
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import printFit
import printSemiType
import virtualType

typealias VirtParamTable = Map<String, Type.NonGenericType>

fun buildTemplateArg(par: TemplateArgContext, paramVirtualTypes: VirtParamTable, typeRepo: TypeRepo): Type.NonGenericType {
    val name = when (val completeName = par.completeName()) {
        null -> return paramVirtualTypes[par.TEMPLATE_PARAM().text]!!
        else -> completeName.fullName().text
    }

    val typeSignature = par.completeName().templateSignature()

    return if (typeSignature == null) {
        typeRepo[name]!!
    } else {
        val typeArgs = typeSignature.templateArg().map { buildTemplateArg(it, paramVirtualTypes, typeRepo) }
        typeRepo.template(name)?.forceStaticApply(typeArgs)!!
    }
}

fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable, typeRepo: TypeRepo): Type.NonGenericType {
    val query = buildFunSignature(funCtx, paramVirtualTypes, typeRepo)
    val funTemplate = typeRepo.functionType(query.inputParameters.size)
    return funTemplate.forceStaticApply(query.allParams)
}

tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable, typeRepo: TypeRepo): Type.NonGenericType {
    val nested = par.funArg()
    return when {
        nested.templateArg() != null -> buildTemplateArg(nested.templateArg(), paramVirtualTypes, typeRepo)
        nested.funSignature() != null -> buildFunArg(nested.funSignature(), paramVirtualTypes, typeRepo)
        else -> buildArg(par.wrappedFunArg(), paramVirtualTypes, typeRepo)
    }
}

fun buildFunSignature(
    funSignature: FunSignatureContext,
    paramVirtualTypes: VirtParamTable,
    typeRepo: TypeRepo
): QueryType {
    val (ins, out) = funSignature.wrappedFunArg().map { buildArg(it, paramVirtualTypes, typeRepo) }.cutLast()
    return QueryType(ins, out)
}

fun parseQuery(query: String, typeRepo: TypeRepo): QueryType {
    val lexer = QueryLexer(CharStreams.fromString(query))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ExceptionErrorListener)

    val parser = QueryParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(ExceptionErrorListener)

    val parseTree: QueryContext = parser.query()

    val typeParams = QueryTypeParameterSelector.visit(parseTree)
    val paramVirtualTypes = typeParams.map{ param ->
        param to virtualType(param, listOf(typeRepo.rootType))
    }.toMap()

    return buildFunSignature(parseTree.funSignature(), paramVirtualTypes, typeRepo)
}

fun main() {
    val repo = createTestRepo()

    println("All Test Types")
    createTestRepo().allTypes.forEach(::printSemiType)

    println("All Test Type Templates")
    createTestRepo().allTemplates.forEach(::printSemiType)

    val collection = repo.template("Collection")!!
    val list = repo.template("List")!!

    val mapFun = FunctionObj(
        info = FunctionInfo("map", "Collections"),
        signature = TypeSignature.GenericSignature(
            typeParameters = listOf( // <T, R>
                repo.typeParam("T"),
                repo.typeParam("R")
            ),
            inputParameters = listOf(
                "collection" to DynamicTypeSubstitution( // Collection<T>
                    collection.forceDynamicApply(
                        ParamSubstitution(0)
                    )
                ),
                "mapper" to DynamicTypeSubstitution(
                    repo.functionType(1).forceDynamicApply(
                        LowerBound(ParamSubstitution(0)), // ? super T
                        UpperBound(ParamSubstitution(1))  // ? extends R
                    )
                )
            ),
            output = DynamicTypeSubstitution(
                list.forceDynamicApply(  // List<R>
                    ParamSubstitution(1)
                )
            )
        )
    )

    while (true) {
        val input = readLine() ?: break
        println("Input: $input")
        try {
            val query = parseQuery(input, repo)
            println("Parsed as: $query")
            printFit(mapFun, query)
        } catch (pce: ParseCancellationException) {
            System.err.println("Failed to parse query: ${pce.message}")
        } catch (other: Exception) {
            System.err.println("Failed to parse query!")
            other.printStackTrace()
        }
    }
}

