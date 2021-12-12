package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ContextManager
import com.mktiti.fsearch.backend.SimpleMapContextManager
import com.mktiti.fsearch.core.cache.CentralInfoCache
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.maven.repo.ExternalMavenDependencyFetcher
import com.mktiti.fsearch.maven.repo.ExternalMavenFetcher
import com.mktiti.fsearch.model.build.util.InMemTypeParseLog
import com.mktiti.fsearch.model.connect.function.JavaFunctionConnector
import com.mktiti.fsearch.model.connect.type.JavaTypeInfoConnector
import com.mktiti.fsearch.modules.ArtifactDependencyFetcher
import com.mktiti.fsearch.modules.ArtifactInfoFetcher
import com.mktiti.fsearch.modules.ArtifactManager
import com.mktiti.fsearch.modules.CachingArtifactStore.Companion.cachedDepsStore
import com.mktiti.fsearch.modules.CachingArtifactStore.Companion.cachedDocsStore
import com.mktiti.fsearch.modules.CachingArtifactStore.Companion.cachedInfoStore
import com.mktiti.fsearch.modules.SecondaryArtifactManager
import com.mktiti.fsearch.modules.docs.DefaultDocManager
import com.mktiti.fsearch.modules.docs.DocManager
import com.mktiti.fsearch.modules.fileystem.GenericFilesystemArtifactStore.Companion.fsDepsStore
import com.mktiti.fsearch.modules.fileystem.GenericFilesystemArtifactStore.Companion.fsDocsStore
import com.mktiti.fsearch.modules.fileystem.GenericFilesystemArtifactStore.Companion.fsInfoStore
import com.mktiti.fsearch.modules.store.ArtifactDepsStoreWrapper
import com.mktiti.fsearch.modules.store.ArtifactDocStoreWrapper
import com.mktiti.fsearch.modules.store.ArtifactInfoStoreWrapper
import com.mktiti.fsearch.parser.docs.JsoupJarHtmlJavadocParser
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object ContextManagerStore {

    lateinit var artifactManager: ArtifactManager
    lateinit var contextManager: ContextManager

    private val log = logger()

    private fun getJclInfo(): Pair<Path, String>? = when (val home = System.getProperty("java.home")) {
        null -> {
            log.error("Java home not set!")
            null
        }
        else -> {
            val version = System.getProperty("java.version")
            val jclHome = if (version.split(".").first().toInt() == 1) {
                // Version <= 8
                home + File.separator + "lib"
            } else {
                // Version >= 9 -> Modularized
                home + File.separator + "jmods"
            }
            Paths.get(jclHome) to version
        }
    }

    fun init(
            storeRoot: Path,
            jclDocLocation: Path?,
            infoCacheLimit: Int, // ~ MB
            docsCacheLimit: Int,  // ~ MB
            depsCacheLimit: Int  // ~ MB
    ) {
        val (libPath, jclVersion) = getJclInfo() ?: return

        if (jclDocLocation == null) {
            log.error("JRE doc location not set!")
        }

        val parseLog = InMemTypeParseLog()

        val javaInfoRepo: JavaInfoRepo = MapJavaInfoRepo

        val typeConnector = JavaTypeInfoConnector(javaInfoRepo, CentralInfoCache, parseLog)
        val funConnector = JavaFunctionConnector(CentralInfoCache)
        val jarHtmlJavadocParser = JsoupJarHtmlJavadocParser(javaInfoRepo, CentralInfoCache)

        val artifactDepsFetcher: ArtifactDependencyFetcher = ExternalMavenDependencyFetcher()
        val artifactFetcher: ArtifactInfoFetcher = ExternalMavenFetcher(
                infoRepo = javaInfoRepo,
                javadocParser = jarHtmlJavadocParser
        )

        fun storeSubDir(name: String) = storeRoot.resolve(name).also {
            Files.createDirectories(it)
        }

        val infoStorePath = storeSubDir("info-store")
        val docsStorePath = storeSubDir("doc-store")
        val depsStorePath = storeSubDir("deps-store")

        val artifactManager: ArtifactManager = SecondaryArtifactManager(
                typeInfoConnector = typeConnector,
                functionConnector = funConnector,
                infoCache = ArtifactInfoStoreWrapper(cachedInfoStore(fsInfoStore(infoStorePath), infoCacheLimit * 1024L)),
                depInfoStore = ArtifactDepsStoreWrapper(cachedDepsStore(fsDepsStore(depsStorePath), depsCacheLimit * 1024L)),
                artifactInfoFetcher = artifactFetcher,
                artifactDepsFetcher = artifactDepsFetcher
        )
        this.artifactManager = artifactManager

        val jclJarPaths = Files.list(libPath).filter {
            it.toFile().extension in listOf("jar", "jmod")
        }.toList()

        log.logInfo {
            "Loading JCL from ${jclJarPaths.joinToString(prefix = "[", postfix = "]") { it.fileName.toString() }}"
        }

        val (jclArtifactRepo, javaRepo) = artifactManager.getOrLoadJcl(jclVersion, jclJarPaths)

        val javadocManager: DocManager = DefaultDocManager(
                cache = ArtifactDocStoreWrapper(cachedDocsStore(fsDocsStore(docsStorePath), docsCacheLimit * 1024L)),
                artifactInfoFetcher = artifactFetcher,
                javadocParser = jarHtmlJavadocParser
        )

        jclDocLocation?.let {
            javadocManager.loadJclDocs(jclVersion, it)
        }

        contextManager = SimpleMapContextManager(
                infoRepo = javaInfoRepo,
                javaRepo = javaRepo,
                jclDomain = jclArtifactRepo,
                artifactManager = artifactManager,
                docManager = javadocManager
        )

        CentralInfoCache.clean()
    }

}