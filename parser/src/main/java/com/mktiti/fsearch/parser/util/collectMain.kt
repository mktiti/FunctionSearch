package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.fitsQuery
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.maven.MavenArtifact
import com.mktiti.fsearch.parser.maven.MavenCollector
import com.mktiti.fsearch.parser.maven.MavenManager
import com.mktiti.fsearch.parser.query.parseQuery
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.type.TwoPhaseCollector
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

fun printLoadResults(typeRepo: TypeRepo, functions: Collection<FunctionObj>) {
    println("==== Loading Done ====")
    println("\tLoaded ${typeRepo.allTypes.size} direct types and ${typeRepo.allTemplates.size} type templates")
    println("\tLoaded ${functions.size} functions")
}

fun printLog(log: InMemTypeParseLog) {
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
}

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
    val jclJarInfo = JarFileInfoCollector.JarInfo("JCL", jarPaths)
    val typeCollector = TwoPhaseCollector(MapJavaInfoRepo, log, JarFileInfoCollector(MapJavaInfoRepo))
    val (javaRepo, jclRepo) = typeCollector.collectJcl("JCL", jclJarInfo)
    val jclFunctions = JarFileFunctionCollector(javaRepo).collectFunctions(jclJarInfo, listOf(jclRepo))

    printLoadResults(jclRepo, jclFunctions)

    val testArtifacts = listOf(
            "org.apache.commons:commons-lang3:3.10",
            "org.apache.commons:commons-text:1.8",
            "org.apache.commons:commons-math3:3.6.1"
    )

    val mavenCollector = MavenCollector(MavenManager.central, log, MapJavaInfoRepo, javaRepo)
    val results = testArtifacts.map {
        val artifact = MavenArtifact.parse(it)!!
        mavenCollector.collectCombined(artifact, listOf(jclRepo)).apply {
            printLoadResults(typeRepo, functions)
        }
    }

    val allFunctions = results.flatMap { it.functions } + jclFunctions

    printLog(log)

    System.gc()

    while (true) {
        print(">")
        val input = readLine() ?: break
        println("Input: $input")
        try {
            val query = parseQuery(input, javaRepo, jclRepo)
            println("Parsed as: $query")
            println("Started searching...")
            allFunctions.asSequence().forEach { function ->
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