package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.maven.MavenArtifact
import com.mktiti.fsearch.parser.maven.MavenCollector
import com.mktiti.fsearch.parser.maven.MavenManager
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryParser
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
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
    // val typeCollector = TwoPhaseCollector(MapJavaInfoRepo, log, JarFileInfoCollector(MapJavaInfoRepo))
    val jclCollector = IndirectJarTypeCollector(MapJavaInfoRepo)
    val (javaRepo, jclRepo) = jclCollector.collectJcl(jclJarInfo, "JCL")
    val jclResolver = SingleRepoTypeResolver(jclRepo)
    val jclFunctions = JarFileFunctionCollector.collectFunctions(jclJarInfo, javaRepo, MapJavaInfoRepo, jclResolver)

    printLoadResults(jclRepo, jclFunctions)

    val testArtifacts = listOf(
            "org.apache.commons:commons-lang3:3.10",
            "org.apache.commons:commons-text:1.8",
            "org.apache.commons:commons-math3:3.6.1"
    )

    val mavenCollector = MavenCollector(MavenManager.central, log, MapJavaInfoRepo)
    val results = testArtifacts.map {
        val artifact = MavenArtifact.parse(it)!!
        mavenCollector.collectCombined(artifact, javaRepo, MapJavaInfoRepo, jclResolver).apply {
            printLoadResults(typeRepo, functions)
        }
    }

    val allFunctions = results.flatMap { it.functions } + jclFunctions
    val typeRepos = results.map { it.typeRepo } + jclRepo

    /*
    typeRepos.flatMap {
        it.allTypes + it.allTemplates
    }.forEach {
        it.samType?.let { sam ->
            println("${it.info} is a SAM type as ${sam.inputs} -> ${sam.output}")
        }
    }
     */

    printLog(log)

    val typeResolver: TypeResolver = SimpleMultiRepoTypeResolver(typeRepos)
    val queryParser: QueryParser = AntlrQueryParser(javaRepo, MapJavaInfoRepo, typeResolver)

    val typePrint = JavaTypePrinter(MapJavaInfoRepo, typeResolver)

    while (true) {
        print(">")
        val input = readLine() ?: break
        println("Input: $input")

        if (input.startsWith(":")) {
            val parts = input.split(" ")
            when (val command = parts[0].drop(1).toLowerCase()) {
                "info" -> {
                    val target = parts.getOrNull(1)?.split("::")
                    val type = target?.getOrNull(0)
                    val function = target?.getOrNull(1)

                    when {
                        type == null -> {
                            println("Missing target")
                        }
                        function == null -> {
                            when (val direct = typeResolver.get(type, allowSimple = true)) {
                                null -> {
                                    when (val template = typeResolver.template(type, allowSimple = true)) {
                                        null -> {
                                            println("Unknown type '$type'")
                                        }
                                        else -> {
                                            typePrint.printSemiType(template)
                                        }
                                    }
                                }
                                else -> {
                                    typePrint.printSemiType(direct)
                                }
                            }
                        }
                        else -> {
                            val foundFun = allFunctions.find {
                                with (it.info) {
                                    name == function && (fileName == type || fileName.split('.').last() == type)
                                }
                            }
                            if (foundFun != null) {
                                typePrint.printFun(foundFun)
                            } else {
                                println("Function '$type::$function' not found")
                            }
                        }
                    }
                }
                "quit" -> {
                    println("Quitting")
                    return
                }
                else -> {
                    println("Unknown command '$command'")
                }
            }
        } else {
            try {
                val (query, virtuals) = queryParser.parse(input)
                print("Parsed as: ")
                typePrint.print(query)
                println("Started searching...")

                val extraResolver = FallbackResolver.withVirtuals(virtuals, typeResolver)
                val fitter = JavaQueryFitter(MapJavaInfoRepo, extraResolver)
                val extraPrint = JavaTypePrinter(MapJavaInfoRepo, extraResolver)

                allFunctions.asSequence().forEach { function ->
                    if (function.info.fileName == "java.util.stream.Stream" && function.info.name == "flatMap") {
                        val a = 0
                    }

                    val result = fitter.fitsQuery(query, function)
                    if (result != null) {
                        print("Fits function ")

                        extraPrint.printFun(function)
                        extraPrint.printFittingMap(result)

                        // typePrint.printFun(function)
                        // typePrint.printFittingMap(result)
                        // println("\tas ${result.funSignature}")
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

}