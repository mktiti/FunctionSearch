package com.mktiti.fsearch.parser.query

import QueryLexer
import QueryParser
import QueryParser.*
import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.fit.virtualType
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.util.TypeException
import com.mktiti.fsearch.core.util.forceStaticApply
import com.mktiti.fsearch.parser.util.ExceptionErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

typealias VirtParamTable = Map<String, Type.NonGenericType>

private fun buildTemplateArg(par: TemplateArgContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val name = when (val completeName = par.completeName()) {
        null -> return paramVirtualTypes[par.TEMPLATE_PARAM().text] ?: error("Type param '${par.TEMPLATE_PARAM().text}' expected")
        else -> completeName.fullName().text
    }

    val typeSignature = par.completeName().templateSignature()

    return if (typeSignature == null) {
        PrimitiveType.fromNameSafe(name)?.let(javaRepo::primitive)
                ?: typeRepo.get(name, allowSimple = true)
                ?: throw TypeException("Simple type $name not found")
    } else {
        val typeArgs = typeSignature.templateArg().map { buildTemplateArg(it, paramVirtualTypes, javaRepo, typeRepo) }
        typeRepo.template(name, allowSimple = true)?.forceStaticApply(typeArgs)
                ?: throw TypeException("Generic type $name not found")
    }
}

private fun buildFunArg(funCtx: FunSignatureContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val query = buildFunSignature(funCtx, paramVirtualTypes, javaRepo, typeRepo)
    val funTemplate = typeRepo.functionType(query.inputParameters.size)
    return funTemplate.forceStaticApply(query.allParams)
}

private tailrec fun buildArg(par: WrappedFunArgContext, paramVirtualTypes: VirtParamTable, javaRepo: JavaRepo, typeRepo: TypeRepo): Type.NonGenericType {
    val nested = par.funArg()

    PrimitiveType.fromNameSafe(nested.text)?.let {
        return javaRepo.primitive(it)
    }

    return when {
        nested.templateArg() != null -> buildTemplateArg(nested.templateArg(), paramVirtualTypes, javaRepo, typeRepo)
        nested.funSignature() != null -> buildFunArg(nested.funSignature(), paramVirtualTypes, javaRepo, typeRepo)
        else -> buildArg(par.wrappedFunArg(), paramVirtualTypes, javaRepo, typeRepo)
    }
}

private fun buildFunSignature(
    funSignature: FunSignatureContext,
    paramVirtualTypes: VirtParamTable,
    javaRepo: JavaRepo,
    typeRepo: TypeRepo
): QueryType {
    fun WrappedFunArgContext.mapArg() = buildArg(this, paramVirtualTypes, javaRepo, typeRepo)

    val inArgs = (funSignature.inArgs().wrappedFunArg() ?: emptyList()).map { it.mapArg() }
    val outArg = funSignature.outArg().wrappedFunArg()?.mapArg() ?: javaRepo.voidType

    return QueryType(inArgs, outArg)
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

