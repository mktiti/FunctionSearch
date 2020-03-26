package query

import FunctionInfo
import FunctionObj
import QueryLexer
import QueryParser
import QueryParser.*
import TypeParameter
import TypeRepo
import TypeSignature
import cutLast
import defaultRepo
import directQuery
import forceDynamicApply
import forceStaticApply
import listType
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import printFit

fun buildStaticArg(par: TemplateArgContext, typeRepo: TypeRepo): Type.NonGenericType {
    val name = par.completeName().fullName().text
    val typeSignature = par.completeName().templateSignature()

    return if (typeSignature == null) {
        typeRepo[name]!!
    } else {
        val typeArgs = typeSignature.templateArg().map { buildStaticArg(it, typeRepo) }
        typeRepo.template(name)?.forceStaticApply(typeArgs)!!
    }
}

fun buildStaticFunArg(funCtx: FunSignatureContext, typeRepo: TypeRepo): Type.NonGenericType {
    val signature = buildStaticFunSignature(funCtx, typeRepo)
    val ins = signature.inputParameters.map { (_, type) -> type }
    val allParams = (ins + signature.output).map { it.type }
    return typeRepo.functionType(ins.size).forceStaticApply(allParams)
}

tailrec fun buildStaticArg(par: WrappedFunArgContext, typeRepo: TypeRepo): Type.NonGenericType {
    val nested = par.funArg()
    return when {
        nested.templateArg() != null -> buildStaticArg(nested.templateArg(), typeRepo)
        nested.funSignature() != null -> buildStaticFunArg(nested.funSignature(), typeRepo)
        else -> buildStaticArg(par.wrappedFunArg(), typeRepo)
    }
}

fun buildStaticFunSignature(
    funSignature: FunSignatureContext,
    typeRepo: TypeRepo
): TypeSignature.DirectSignature {
    val (ins, out) = funSignature.wrappedFunArg().map { buildStaticArg(it, typeRepo) }.cutLast()
    return directQuery(ins, out)
}

fun buildGenericFunSignature(
    funSignature: FunSignatureContext,
    typeParams: List<TypeParameter>,
    typeRepo: TypeRepo
): TypeSignature {
    TODO("Generic query building")
}

fun buildFunSignature(funSignature: FunSignatureContext, typeParams: List<TypeParameter>, typeRepo: TypeRepo): TypeSignature {
    return if (typeParams.isEmpty()) {
        buildStaticFunSignature(funSignature, typeRepo)
    } else {
        buildGenericFunSignature(funSignature, typeParams, typeRepo)
    }
}

fun parseQuery(query: String, typeRepo: TypeRepo): TypeSignature {
    val lexer = QueryLexer(CharStreams.fromString(query))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ExceptionErrorListener)

    val parser = QueryParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(ExceptionErrorListener)

    val parseTree: QueryContext = parser.query()

    val typeParams = QueryTypeParameterSelector.visit(parseTree).map { typeRepo.typeParam(it) }

    return buildFunSignature(parseTree.funSignature(), typeParams, typeRepo)
}

fun main() {
    val mapFun = FunctionObj(
        info = FunctionInfo("map", "List"),
        signature = TypeSignature.GenericSignature(
            typeParameters = listOf(defaultRepo.typeParam("T"), defaultRepo.typeParam("R")),
            inputParameters = listOf(
                "list" to ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                    listType.forceDynamicApply(
                        ApplicationParameter.ParamSubstitution(0)
                    )
                ),
                "mapper" to ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                    defaultRepo.functionType(1).forceDynamicApply(ApplicationParameter.ParamSubstitution(0),
                        ApplicationParameter.ParamSubstitution(1)
                    )
                )
            ),
            output = ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution(
                listType.forceDynamicApply(
                    ApplicationParameter.ParamSubstitution(1)
                )
            )
        )
    )

    while (true) {
        val input = readLine() ?: break
        println("Input: $input")
        try {
            val signature = parseQuery(input, defaultRepo)
            println(signature.fullString)
            printFit(mapFun, signature)
        } catch (pce: ParseCancellationException) {
            System.err.println("Failed to parse query: ${pce.message}")
        } catch (other: Exception) {
            System.err.println("Failed to parse query!")
            other.printStackTrace()
        }
    }
}

