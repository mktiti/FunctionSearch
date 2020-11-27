package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.javadoc.DocStore
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.flatAll
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.core.util.show.TypePrint
import com.mktiti.fsearch.maven.repo.ExternalMavenFetcher
import com.mktiti.fsearch.maven.repo.MavenMapArtifactManager
import com.mktiti.fsearch.maven.repo.MavenMapJavadocManager
import com.mktiti.fsearch.modules.*
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryParser
import com.mktiti.fsearch.parser.service.FunctionCollector
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.util.InMemTypeParseLog
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

fun printLoadResults(typeRepo: TypeRepo, functions: FunctionCollector.FunctionCollection) {
    println("==== Loading Done ====")
    println("\tLoaded ${typeRepo.allTypes.size} direct types and ${typeRepo.allTemplates.size} type templates")
    println("\tLoaded ${functions.staticFunctions.size} static functions")
    println("\tLoaded ${functions.instanceMethods.flatAll().count()} instance functions")
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

private data class QueryContext(
        val infoRepo: JavaInfoRepo,
        val javaRepo: JavaRepo,
        val artifacts: Set<ArtifactId>,
        val domain: DomainRepo,
        val docStore: DocStore
) {

    val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, domain.typeResolver)
    val fitter: QueryFitter = JavaQueryFitter(infoRepo, domain.typeResolver)
    val typePrint: TypePrint = JavaTypePrinter(infoRepo, domain.typeResolver, docStore)

    fun withVirtuals(virtuals: Collection<DirectType>) = copy(
            domain = FunFallbackDomainRepo(FallbackResolver.withVirtuals(virtuals, domain.typeResolver), domain)
    )

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
    val jclCollector = IndirectJarTypeCollector(MapJavaInfoRepo)
    val (javaRepo, jclRepo) = jclCollector.collectJcl(jclJarInfo, "JCL")
    val jclResolver = SingleRepoTypeResolver(jclRepo)
    val jclFunctions = JarFileFunctionCollector.collectFunctions(jclJarInfo, javaRepo, MapJavaInfoRepo, jclResolver)

    printLoadResults(jclRepo, jclFunctions)

    printLog(log)

    val jclArtifactRepo = SimpleDomainRepo(jclResolver, jclFunctions)

    val mavenManager = MavenMapArtifactManager(
            typeCollector = JarFileInfoCollector(MapJavaInfoRepo),
            funCollector = JarFileFunctionCollector,
            infoRepo = MapJavaInfoRepo,
            javaRepo = javaRepo,
            baseResolver = jclResolver,
            mavenFetcher = ExternalMavenFetcher()
    )

    val mavenJavadocManager = MavenMapJavadocManager(
            infoRepo = MapJavaInfoRepo,
            mavenFetcher = ExternalMavenFetcher()
    )

    var context = QueryContext(
            infoRepo = MapJavaInfoRepo,
            javaRepo = javaRepo,
            artifacts = emptySet(),
            domain = jclArtifactRepo,
            docStore = DocStore.nop()
    )

    fun test(iterations: Int) {
        val (query, virtuals) = context.queryParser.parse("List<a>, int -> a")
        val updated = context.withVirtuals(virtuals)
        measureTimeMillis {
            repeat(iterations) {
                updated.fitter.findFittings(query, updated.domain.staticFunctions, updated.domain.instanceFunctions).count()
            }
        }.apply {
            println("Time for $iterations iterations: $this ms")
        }
    }

    while (true) {
        val iters = readLine()?.toIntOrNull()
        if (iters != null) {
            test(iters)
        } else {
            println("Input number of iterations!")
        }
    }

    while (true) {
        print(">")
        val input = readLine() ?: break
        println("Input: $input")

        if (input.startsWith(":")) {
            val parts = input.split(" ")
            when (val command = parts[0].drop(1).toLowerCase()) {
                "info" -> {
                    printInfo(parts.drop(1), context)
                }
                "quit" -> {
                    println("Quitting")
                    return
                }
                "load" -> {
                    val artifact = parts.getOrNull(1)?.let {
                        ArtifactId.parse(it)
                    }
                    if (artifact == null) {
                        println("Failed to parse artifact info!")
                    } else {
                        if (artifact !in context.artifacts) {
                            val combined = context.artifacts + artifact
                            val loaded = mavenManager.getWithDependencies(combined)

                            println("Loaded artifact $artifact")

                            val docStore = mavenJavadocManager.forArtifacts(combined)

                            context = context.copy(
                                    artifacts = combined,
                                    domain = FallbackDomainRepo(
                                            repo = loaded,
                                            fallbackRepo = loaded
                                    ), docStore = docStore
                            )
                        } else {
                            println("Artifact $artifact already loaded")
                        }
                    }
                }
                else -> {
                    println("Unknown command '$command'")
                }
            }
        } else {
            try {
                val (query, virtuals) = context.queryParser.parse(input)
                print("Parsed as: ")
                context.typePrint.print(query)
                println("Started searching...")

                val extraContext = context.withVirtuals(virtuals)

                context.domain.allFunctions.map { function ->
                    extraContext.fitter.fitsQuery(query, function)?.let { function to it }
                }.filter { it != null }.collect(Collectors.toList()).filterNotNull().forEach { (function, result) ->
                    print("Fits function ")
                    extraContext.typePrint.printFun(function)
                    extraContext.typePrint.printFittingMap(result)
                    context.docStore[function.info]?.let {
                        println()
                        println("===> Doc:")
                        println(it.longInfo)
                        println()
                    }
                }

                /*
                context.allFunctions.asSequence().forEach { function ->
                    if (function.info.fileName == "java.util.Collections" && function.info.name == "sort") {
                        val a = 0
                    }

                    val result = extraContext.fitter.fitsQuery(query, function)
                    if (result != null) {
                        print("Fits function ")

                        extraContext.typePrint.printFun(function)
                        extraContext.typePrint.printFittingMap(result)

                        // typePrint.printFun(function)
                        // typePrint.printFittingMap(result)
                        // println("\tas ${result.funSignature}")
                    }
                }
                 */
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

private fun printInfo(command: List<String>, context: QueryContext) {
    val target = command.getOrNull(0)?.split("::")
    val type = target?.getOrNull(0)
    val function = target?.getOrNull(1)

    val resolver = context.domain.typeResolver

    when {
        type == null -> {
            println("Missing target")
        }
        function == null -> {
            when (val direct = resolver.get(type, allowSimple = true)) {
                null -> {
                    when (val template = resolver.template(type, allowSimple = true)) {
                        null -> {
                            println("Unknown type '$type'")
                        }
                        else -> {
                            context.typePrint.printSemiType(template)
                        }
                    }
                }
                else -> {
                    context.typePrint.printSemiType(direct)
                }
            }
        }
        else -> {
            val foundFun = context.domain.allFunctions.filter {
                with (it.info) {
                    name == function && (file.fullName == type || file.simpleName == type)
                }
            }.findAny().orElseGet(null)
            if (foundFun != null) {
                context.typePrint.printFun(foundFun)
            } else {
                println("Function '$type::$function' not found")
            }
        }
    }
}