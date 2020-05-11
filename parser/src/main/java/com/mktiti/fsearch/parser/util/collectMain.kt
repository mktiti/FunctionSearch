package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.fit.fitsQuery
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.parser.function.AsmParser
import com.mktiti.fsearch.parser.query.parseQuery
import com.mktiti.fsearch.parser.type.JarTypeCollector
import com.mktiti.fsearch.parser.type.TwoPhaseCollector
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun main(args: Array<String>) {
    val libPath = args.getOrElse(0) {
        println("Using Java home's lib directory as JCL base")
        when (val home = System.getProperty("java.home")) {
            null -> {
                System.err.println("Please pass the path of the JRE lib/ as the first argument!")
                return
            }
            else -> {
                if (System.getProperty("java.version").split(".").first().toInt() == 1) {
                    // Version <= 8
                    home + File.separator + "lib"
                } else {
                    // Version >= 9 -> Modularized
                    home + File.separator + "jmods"
                }
            }
        }
    }.let { Paths.get(it) }

    val jarPaths = Files.list(libPath).filter {
        it.toFile().extension in listOf("jar", "jmod")
    }.toList()
    println("Loading JCL from ${jarPaths.joinToString(prefix = "[", postfix = "]") { it.fileName.toString() }}")

    val log = InMemTypeParseLog()

    println("==== Loading JCL ====")
    val typeCollector: JarTypeCollector = TwoPhaseCollector(MapJavaInfoRepo, log)
    val (javaRepo, jclRepo) = typeCollector.collectJcl("JCL", jarPaths)

    println("==== Loading Done ====")
    println("\tLoaded ${jclRepo.allTypes.size} direct types and ${jclRepo.allTemplates.size} type templates")
    println("\t${log.allCount} warnings")

    println("\t== Type not found errors (${log.typeNotFounds.size}):")
    log.typeNotFounds.groupBy { it.used }.forEach { (used, users) ->
        println("\t\t$used used by $users")
    }

    println("\t== Raw type usages (${log.rawUsages.size})")
    println("\t== Application errors (${log.applicableErrors.size})")
    log.applicableErrors.forEach { (user, used) ->
        println("\t\t$used used by $user")
    }

    println("==== Loading Functions ====")
    val functions = AsmParser(javaRepo, jclRepo).loadFunctions(jarPaths)
    println("==== Loading Done ====")
    println("\tLoaded ${functions.size} functions")

    /*functions.forEach { function ->
        println(function)
    }
     */

    while (true) {
        print(">")
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