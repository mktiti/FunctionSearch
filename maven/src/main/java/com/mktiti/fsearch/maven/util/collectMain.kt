package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.core.util.show.JavaTypeStringResolver
import com.mktiti.fsearch.core.util.show.TypePrint
import com.mktiti.fsearch.maven.ArtifactRepo
import com.mktiti.fsearch.maven.MavenArtifact
import com.mktiti.fsearch.maven.MavenCollector
import com.mktiti.fsearch.maven.SimpleArtifactRepo
import com.mktiti.fsearch.maven.repo.MavenManager
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryParser
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.util.InMemTypeParseLog
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

private data class QueryContext(
        val infoRepo: JavaInfoRepo,
        val javaRepo: JavaRepo,
        val allRepos: Collection<ArtifactRepo>,
        val resolver: TypeResolver = SimpleMultiRepoTypeResolver(allRepos.map { it.typeRepo })
) {

    val allFunctions: Collection<FunctionObj> = allRepos.flatMap { it.functions }
    val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, resolver)
    val fitter: QueryFitter = JavaQueryFitter(infoRepo, resolver)
    val typePrint: TypePrint = JavaTypePrinter(infoRepo, resolver)

    private fun setRepos(repos: Collection<ArtifactRepo>): QueryContext = copy(
            allRepos = repos,
            resolver = SimpleMultiRepoTypeResolver(repos.map { it.typeRepo })
    )

    operator fun plus(repo: ArtifactRepo) = setRepos(allRepos + repo)

    operator fun plus(repos: Collection<ArtifactRepo>) = setRepos(allRepos + repos)

    operator fun minus(info: MavenArtifact) = setRepos(allRepos.filter { it.info != info })

    fun withVirtuals(virtuals: Collection<DirectType>) = copy(
            resolver = FallbackResolver.withVirtuals(virtuals, resolver)
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

    val jclArtifactRepo = SimpleArtifactRepo(
            info = MavenArtifact(group = listOf("openjdk"), name = "jcl", version = System.getProperty("java.version")),
            typeRepo = jclRepo,
            functions = jclFunctions
    )

    val mavenCollector = MavenCollector(MapJavaInfoRepo)

    var context = QueryContext(MapJavaInfoRepo, javaRepo, listOf(jclArtifactRepo))

    while (true) {
        print(">")
        val input = readLine() ?: break
        println("Input: $input")

        if (input.startsWith(":")) {
            val parts = input.split(" ")
            when (val command = parts[0].drop(1).toLowerCase()) {
                "info" -> {
                    printInfo(parts.drop(1), context.resolver, context.allFunctions)
                }
                "quit" -> {
                    println("Quitting")
                    return
                }
                "load" -> {
                    val artifact = parts.getOrNull(1)?.let {
                        MavenArtifact.parse(it)
                    }
                    if (artifact == null) {
                        println("Failed to parse artifact info!")
                    } else {
                        mavenCollector.collectCombined(artifact, javaRepo, MapJavaInfoRepo, jclResolver).apply {
                            forEach {
                                printLoadResults(it.typeRepo, it.functions)
                            }

                            context += this
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

private fun printInfo(command: List<String>, typeResolver: TypeResolver, allFunctions: Collection<FunctionObj>) {
    val target = command.getOrNull(0)?.split("::")
    val type = target?.getOrNull(0)
    val function = target?.getOrNull(1)

    val typeStringResolver = JavaTypeStringResolver(
            MapJavaInfoRepo
    )

    val typePrint = JavaTypePrinter(
            infoRepo = MapJavaInfoRepo,
            typeResolver = typeResolver,
            stringResolver = typeStringResolver
    )

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