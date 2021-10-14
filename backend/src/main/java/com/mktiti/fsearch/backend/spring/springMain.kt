@file:JvmName("SpringStart")

package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ContextManager
import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.SimpleMapContextManager
import com.mktiti.fsearch.backend.grpc.GrpcArtifactService
import com.mktiti.fsearch.backend.grpc.GrpcInfoService
import com.mktiti.fsearch.backend.grpc.GrpcSearchService
import com.mktiti.fsearch.core.javadoc.FunDocResolver
import com.mktiti.fsearch.core.javadoc.SingleDocMapStore
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.core.util.flatAll
import com.mktiti.fsearch.maven.repo.ExternalMavenDependencyFetcher
import com.mktiti.fsearch.maven.repo.ExternalMavenFetcher
import com.mktiti.fsearch.maven.util.JarHtmlJavadocParser
import com.mktiti.fsearch.model.build.service.FunctionCollection
import com.mktiti.fsearch.model.build.service.TypeInfoTypeParamResolver
import com.mktiti.fsearch.model.build.util.InMemTypeParseLog
import com.mktiti.fsearch.model.connect.function.JavaFunctionConnector
import com.mktiti.fsearch.model.connect.type.JavaTypeInfoConnector
import com.mktiti.fsearch.modules.*
import com.mktiti.fsearch.modules.fileystem.FilesystemArtifactDocStore
import com.mktiti.fsearch.modules.fileystem.FilesystemArtifactInfoStore
import com.mktiti.fsearch.parser.function.JarFileFunctionInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.web.context.annotation.ApplicationScope
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

@SpringBootApplication
@Import(GrpcSearchService::class, GrpcInfoService::class, GrpcArtifactService::class)
class SpringMain {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().components(Components()).info(Info().apply {
        title = "FunctionSearch"
        version = ProjectInfo.version.removeSuffix("-SNAPSHOT")
    })

}

internal fun printLoadResults(typeRepo: TypeRepo, functions: FunctionCollection) {
    println("==== Loading Done ====")
    println("\tLoaded ${typeRepo.allTypes.size} direct types and ${typeRepo.allTemplates.size} type templates")
    println("\tLoaded ${functions.staticFunctions.size} static functions")
    println("\tLoaded ${functions.instanceMethods.flatAll().count()} instance functions")
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

object ContextManagerStore {

    lateinit var artifactManager: ArtifactManager
    lateinit var contextManager: ContextManager

    fun init(
            storeRoot: Path,
            jclDocLocation: Path?
    ) {
        val libPath = when (val home = System.getProperty("java.home")) {
            null -> {
                System.err.println("Java home not set!")
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
        }.let(Paths::get)

        if (jclDocLocation == null) {
            println("JRE doc location not set")
        }

        val jarPaths = Files.list(libPath).filter {
            it.toFile().extension in listOf("jar", "jmod")
        }.toList()
        println("Loading JCL from ${jarPaths.joinToString(prefix = "[", postfix = "]") { it.fileName.toString() }}")

        val log = InMemTypeParseLog()

        println("==== Loading JCL ====")

        val jarTypeLoader = JarFileInfoCollector(MapJavaInfoRepo)
        val jarFunLoader = JarFileFunctionInfoCollector(MapJavaInfoRepo)

        val typeConnector = JavaTypeInfoConnector(MapJavaInfoRepo, log)
        val funConnector = JavaFunctionConnector

        val jclJarInfo = JarInfo("JCL", jarPaths)
        val rawTypeInfo = jarTypeLoader.collectTypeInfo(jclJarInfo)
        val typeParamResolver = TypeInfoTypeParamResolver(rawTypeInfo.templateInfos)

        val rawFunInfo = jarFunLoader.collectFunctions(jclJarInfo, typeParamResolver)

        val (javaRepo, jclRepo) = typeConnector.connectJcl(rawTypeInfo)
        val funs = funConnector.connect(rawFunInfo)

        val jclResolver = SingleRepoTypeResolver(jclRepo)

        val jclDocs = jclDocLocation?.let {
            val docMap = JarHtmlJavadocParser(MapJavaInfoRepo).parseJar(it.toFile()) ?: return@let null
            SingleDocMapStore(docMap.convertMap())
        } ?: FunDocResolver.nop()

        printLoadResults(jclRepo, funs)

        printLog(log)

        val jclArtifactRepo = SimpleDomainRepo(jclResolver, funs)

        val artifactFetcher: ArtifactInfoFetcher = ExternalMavenFetcher(infoRepo = MapJavaInfoRepo)

        val infoStorePath = storeRoot.resolve("info-store").also {
            Files.createDirectories(it)
        }
        val docsStorePath = storeRoot.resolve("doc-store").also {
            Files.createDirectories(it)
        }

        val artifactManager: ArtifactManager = SecondaryArtifactManager(
                typeInfoConnector = typeConnector,
                functionConnector = funConnector,
                infoCache = FilesystemArtifactInfoStore(infoStorePath),
                depInfoFetcher = ExternalMavenDependencyFetcher(),
                artifactInfoFetcher = artifactFetcher
        )
        this.artifactManager = artifactManager

        val javadocManager: DocManager = DefaultDocManager(
                jclDocs = jclDocs,
                cache = FilesystemArtifactDocStore(docsStorePath),
                artifactInfoFetcher = artifactFetcher
        )

        contextManager = SimpleMapContextManager(
                infoRepo = MapJavaInfoRepo,
                javaRepo = javaRepo,
                jclDomain = jclArtifactRepo,
                artifactManager = artifactManager,
                docManager = javadocManager
        )

        // Force load apache commons and guava
        /*contextManager.context(setOf(
                ArtifactId(listOf("org", "apache", "commons"), "commons-lang3", "3.11"),
                ArtifactId(listOf("com", "google", "guava"), "guava", "30.0-jre")
        ))
         */
    }

}

@ApplicationScope
class InitializerBean(
        @Value("data-store.path") private val storeBasePath: String,
        @Value("data-store.path") private val javadocPath: String?
) {
    @EventListener(ApplicationEnvironmentPreparedEvent::class)
    fun onStartup() {
        ContextManagerStore.init(
                storeRoot = Paths.get(storeBasePath),
                jclDocLocation = javadocPath?.let(Paths::get)
        )
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(SpringMain::class.java, *args)
}
