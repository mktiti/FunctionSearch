package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.fit.fitsQuery
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.parser.function.AsmParser
import com.mktiti.fsearch.parser.query.parseQuery
import com.mktiti.fsearch.parser.type.JarTypeCollector
import com.mktiti.fsearch.parser.type.TwoPhaseCollector
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.nio.file.Paths

fun main(args: Array<String>) {
    val rtPath = args.getOrNull(0)?.let { Paths.get(it) }
    if (rtPath == null) {
        System.err.println("Please pass the path of a JAR (rt.jar from the JRE) as the first argument!")
        return
    }

    val typeCollector: JarTypeCollector = TwoPhaseCollector(MapJavaInfoRepo)
    val (javaRepo, jclRepo) = typeCollector.collectJcl("JCL", rtPath)

    println("===============")
    println("Loaded types:")
    /*jclRepo.allTypes.forEach {
        printSemiType(it)
    }
     */

    val functions = AsmParser(javaRepo, jclRepo).loadFunctions(rtPath)

    println("===============")
    println("Loaded functions (${functions.size}):")
    /*functions.forEach { function ->
        println(function)
    }
     */

    while (true) {
        val input = readLine() ?: break
        println("Input: $input")
        try {
            val query = parseQuery(input, javaRepo, jclRepo)
            println("Parsed as: $query")
            println("Started searching...")
            functions.asSequence().forEach { function ->
                val result = fitsQuery(query, function)
                if (result != null) {
                    println("Fits function $function")
                    println("\tas ${result.funSignature}")
                }
            }
            println("Search done!")

        } catch (pce: ParseCancellationException) {
            System.err.println("Failed to parse query: ${pce.message}")
        } catch (other: Exception) {
            System.err.println("Failed to parse query!")
            other.printStackTrace()
        }
    }

}