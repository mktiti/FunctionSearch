package com.mktiti.fsearch.parser.util

import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.fit.JavaQueryFitter
import com.mktiti.fsearch.core.fit.QueryFitter
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.show.JavaTypePrinter
import com.mktiti.fsearch.core.util.show.TypePrint
import com.mktiti.fsearch.parser.connect.FunctionCollection
import com.mktiti.fsearch.parser.connect.function.JavaFunctionConnector
import com.mktiti.fsearch.parser.connect.type.JavaTypeInfoConnector
import com.mktiti.fsearch.parser.intermediate.TypeInfoTypeParamResolver
import com.mktiti.fsearch.parser.intermediate.function.JarFileFunctionInfoCollector
import com.mktiti.fsearch.parser.intermediate.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.query.AntlrQueryParser
import com.mktiti.fsearch.parser.query.QueryImports
import com.mktiti.fsearch.parser.query.QueryParser
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

internal fun printLoadResults(typeRepo: TypeRepo, functions: FunctionCollection) {
    println("==== Loading Done ====")
    println("\tLoaded ${typeRepo.allTypes.size} direct types and ${typeRepo.allTemplates.size} type templates")
    println("\tLoaded ${functions.staticFunctions.size} static functions")
    println("\tLoaded ${functions.instanceMethods.flatMap { it.value }.count()} instance functions")
}

internal fun printLog(log: InMemTypeParseLog) {
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
        val resolver: TypeResolver,
        val allFunctions: Collection<FunctionObj>
) {

    val queryParser: QueryParser = AntlrQueryParser(javaRepo, infoRepo, resolver)
    val fitter: QueryFitter = JavaQueryFitter(infoRepo, resolver)
    val typePrint: TypePrint = JavaTypePrinter(infoRepo, resolver)

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

    val jarTypeLoader = JarFileInfoCollector(MapJavaInfoRepo)
    val jarFunLoader = JarFileFunctionInfoCollector

    val typeConnector = JavaTypeInfoConnector(MapJavaInfoRepo, log)
    val funConnector = JavaFunctionConnector

    val jclJarInfo = JarFileInfoCollector.JarInfo("JCL", jarPaths)
    val rawTypeInfo = jarTypeLoader.collectTypeInfo(jclJarInfo)
    val typeParamResolver = TypeInfoTypeParamResolver(rawTypeInfo.templateInfos)

    val rawFunInfo = jarFunLoader.collectFunctions(jclJarInfo, MapJavaInfoRepo, typeParamResolver)

    val (javaRepo, jclRepo) = typeConnector.connectJcl(rawTypeInfo)
    val funs = funConnector.connect(rawFunInfo)

    val jclResolver = SingleRepoTypeResolver(jclRepo)

    printLoadResults(jclRepo, funs)

    printLog(log)


    val context = QueryContext(
            infoRepo = MapJavaInfoRepo,
            javaRepo = javaRepo,
            resolver = jclResolver,
            allFunctions = funs.staticFunctions + funs.instanceMethods.flatMap { it.value }
    )

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
                else -> {
                    println("Unknown command '$command'")
                }
            }
        } else {
            try {
                val (query, virtuals) = context.queryParser.parse(input, QueryImports.none)
                print("Parsed as: ")
                context.typePrint.print(query)
                println("Started searching...")

                val extraContext = context.withVirtuals(virtuals)

                context.allFunctions.mapNotNull { function ->
                    extraContext.fitter.fitsQuery(query, function)?.let { function to it }
                }.forEach { (function, result) ->
                    print("Fits function ")
                    extraContext.typePrint.printFun(function)
                    extraContext.typePrint.printFittingMap(result)
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

private fun printInfo(command: List<String>, context: QueryContext) {
    val target = command.getOrNull(0)?.split("::")
    val type = target?.getOrNull(0)
    val function = target?.getOrNull(1)

    when {
        type == null -> {
            println("Missing target")
        }
        function == null -> {
            when (val direct = context.resolver.get(type, allowSimple = true)) {
                null -> {
                    when (val template = context.resolver.template(type, allowSimple = true)) {
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
            val foundFun = context.allFunctions.find {
                with (it.info) {
                    name == function && (file.fullName == type || file.simpleName == type)
                }
            }
            if (foundFun != null) {
                context.typePrint.printFun(foundFun)
            } else {
                println("Function '$type::$function' not found")
            }
        }
    }
}
