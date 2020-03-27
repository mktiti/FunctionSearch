package query

import ApplicationParameter
import FunctionInfo
import FunctionObj
import QueryLexer
import QueryParser
import QueryParser.*
import QueryType
import Type
import TypeParameter
import TypeRepo
import TypeSignature
import cutLast
import defaultRepo
import forceDynamicApply
import forceStaticApply
import listType
import lowerBounds
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import printFit
import upperBounds
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
    val mapFun = FunctionObj(
        info = FunctionInfo("map", "List"),
        signature = TypeSignature.GenericSignature(
            typeParameters = listOf( // <T, R, ? sup T, ? ext R>
                defaultRepo.typeParam("T", defaultRepo.defaultTypeBounds),
                defaultRepo.typeParam("R", defaultRepo.defaultTypeBounds),
                TypeParameter("\$supT", lowerBounds(ApplicationParameter.ParamSubstitution(0))),
                TypeParameter("\$extR", upperBounds(ApplicationParameter.ParamSubstitution(1)))
            ),
            inputParameters = listOf( // (list: List<T>, mapper: Fn<? sup T, ? ext R>)
                "list" to ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                    listType.forceDynamicApply(
                        ApplicationParameter.ParamSubstitution(0)
                    )
                ),
                "mapper" to ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                    defaultRepo.functionType(1).forceDynamicApply(
                        ApplicationParameter.ParamSubstitution(2), // ? sup T
                        ApplicationParameter.ParamSubstitution(3)  // ? ext R
                    )
                )
            ),
            output = ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                listType.forceDynamicApply(
                    ApplicationParameter.ParamSubstitution(1)
                )
            ) // -> List<R>
        )
    )

    while (true) {
        val input = readLine() ?: break
        println("Input: $input")
        try {
            val query = parseQuery(input, defaultRepo)
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

