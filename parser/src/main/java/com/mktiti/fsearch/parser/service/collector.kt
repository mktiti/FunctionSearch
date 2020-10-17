package com.mktiti.fsearch.parser.service

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.repo.*

interface JclCollector<in I> {

    data class Result(val javaRepo: JavaRepo, val jclRepo: TypeRepo)

    fun collectJcl(info: I, name: String): Result

}

interface JarTypeCollector<in I> {

    companion object {
        fun empty(): TypeRepo = SetTypeRepo()

        fun <I> nop() = object : JarTypeCollector<I> {
            override fun collectArtifact(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver) = empty()
        }
    }

    fun collectArtifact(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver): TypeRepo

}

interface FunctionCollector<in I> {

    companion object {
        fun empty(): Collection<FunctionObj> = emptyList()

        fun <I> nop() = object : FunctionCollector<I> {
            override fun collectFunctions(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver) = empty()
        }
    }

    fun collectFunctions(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver): Collection<FunctionObj>

}

interface CombinedCollector<in I> {

    companion object {
        fun <I> nop() = object : CombinedCollector<I> {
            override fun collectCombined(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver) = Result(
                    JarTypeCollector.empty(), FunctionCollector.empty()
            )
        }
    }

    data class Result(
            val typeRepo: TypeRepo,
            val functions: Collection<FunctionObj>
    )

    fun collectCombined(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver): Result

}

class FallbackCombinedCollector<in I>(
        private val typeCollector: JarTypeCollector<I>,
        private val functionCollector: FunctionCollector<I>
) : CombinedCollector<I> {

    override fun collectCombined(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver): CombinedCollector.Result {
        val types = typeCollector.collectArtifact(info, javaRepo, dependencyResolver)
        val extendedResolver = FallbackResolver(types, dependencyResolver)
        val functions = functionCollector.collectFunctions(info, javaRepo, extendedResolver)
        return CombinedCollector.Result(types, functions)
    }

}