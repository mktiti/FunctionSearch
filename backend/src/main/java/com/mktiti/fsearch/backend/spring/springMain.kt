@file:JvmName("SpringStart")

package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ContextManager
import com.mktiti.fsearch.backend.SimpleMapContextManager
import com.mktiti.fsearch.core.javadoc.DocStore
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.maven.repo.ExternalMavenFetcher
import com.mktiti.fsearch.maven.repo.MavenMapArtifactManager
import com.mktiti.fsearch.maven.repo.MavenMapJavadocManager
import com.mktiti.fsearch.maven.util.JarHtmlJavadocParser
import com.mktiti.fsearch.maven.util.printLoadResults
import com.mktiti.fsearch.maven.util.printLog
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.SimpleDomainRepo
import com.mktiti.fsearch.parser.function.JarFileFunctionCollector
import com.mktiti.fsearch.parser.type.IndirectJarTypeCollector
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.parser.util.InMemTypeParseLog
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringMain

object ContextManagerStore {

    lateinit var contextManager: ContextManager

    fun init(args: List<String>) {
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

        val jclDocLocation = args.getOrNull(0)?.let(Paths::get)
        if (jclDocLocation == null) {
            println("JRE doc location not set")
        }

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

        val jclDocs = jclDocLocation?.let {
            JarHtmlJavadocParser(MapJavaInfoRepo).parseJar(it.toFile())
        }

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
                mavenFetcher = ExternalMavenFetcher(),
                jclDocs = jclDocs ?: DocStore.nop()
        )

        contextManager = SimpleMapContextManager(
                infoRepo = MapJavaInfoRepo,
                javaRepo = javaRepo,
                jclDomain = jclArtifactRepo,
                artifactManager = mavenManager,
                docManager = mavenJavadocManager
        )

        // Force load apache commons and guava
        contextManager.context(setOf(
                ArtifactId(listOf("org", "apache", "commons"), "commons-lang3", "3.11"),
                ArtifactId(listOf("com", "google", "guava"), "guava", "30.0-jre")
        ))
    }

}

fun main(args: Array<String>) {
    ContextManagerStore.init(args.toList())
    SpringApplication.run(SpringMain::class.java, *args)
}
