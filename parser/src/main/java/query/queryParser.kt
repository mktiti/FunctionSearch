package query

import ExceptionErrorListener
import PrimitiveType
import QueryLexer
import QueryParser
import QueryParser.*
import QueryType
import Type
import cutLast
import forceStaticApply
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import repo.JavaRepo
import repo.TypeRepo
import virtualType

typealias VirtParamTable = Map<String, Type.NonGenericType>

fun buildTemplateArg(par: TemplateArgContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val name = when (val completeName = par.completeName()) {
        null -> return paramVirtualTypes[par.TEMPLATE_PARAM().text] ?: error("Type param '${par.TEMPLATE_PARAM().text}' expected")
        else -> completeName.fullName().text
    }

    val typeSignature = par.completeName().templateSignature()

    return if (typeSignature == null) {
        PrimitiveType.fromSignatureSafe(name)?.let(javaRepo::primitive) ?: typeRepo[name]!!
    } else {
        val typeArgs = typeSignature.templateArg().map { buildTemplateArg(it, paramVirtualTypes, javaRepo, typeRepo) }
        typeRepo.template(name)?.forceStaticApply(typeArgs)!!
    }
}

fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val query = buildFunSignature(funCtx, paramVirtualTypes, javaRepo, typeRepo)
    val funTemplate = typeRepo.functionType(query.inputParameters.size)
    return funTemplate.forceStaticApply(query.allParams)
}

tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val nested = par.funArg()
    return when {
        nested.templateArg() != null -> buildTemplateArg(nested.templateArg(), paramVirtualTypes, javaRepo, typeRepo)
        nested.funSignature() != null -> buildFunArg(nested.funSignature(), paramVirtualTypes, javaRepo, typeRepo)
        else -> buildArg(par.wrappedFunArg(), paramVirtualTypes, javaRepo, typeRepo)
    }
}

fun buildFunSignature(
    funSignature: FunSignatureContext,
    paramVirtualTypes: VirtParamTable,
    javaRepo: JavaRepo,
    typeRepo: TypeRepo
): QueryType {
    val (ins, out) = funSignature.wrappedFunArg().map { buildArg(it, paramVirtualTypes, javaRepo, typeRepo) }.cutLast()
    return QueryType(ins, out)
}

fun parseQuery(query: String, javaRepo: JavaRepo, typeRepo: TypeRepo): QueryType {
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

    return buildFunSignature(parseTree.funSignature(), paramVirtualTypes, javaRepo, typeRepo)
}

