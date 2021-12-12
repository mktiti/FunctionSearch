package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.maven.util.DependencyUtil
import com.mktiti.fsearch.maven.util.IoUtil
import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.*
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactInfoFetcher
import com.mktiti.fsearch.parser.function.JarFileFunctionInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.util.logTrace
import com.mktiti.fsearch.util.logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ExternalMavenFetcher(
        private val basePath: Path = IoUtil.tempDir("fsearch-external-maven-fetch-"),
        private val infoRepo: JavaInfoRepo,
        private val jarFileInfoCollector: ArtifactTypeInfoCollector<JarInfo> = JarFileInfoCollector(infoRepo),
        private val jarFunctionInfoCollector: FunctionInfoCollector<JarInfo> = JarFileFunctionInfoCollector(infoRepo),
        private val javadocParser: JarHtmlJavadocParser
) : ArtifactInfoFetcher {

    companion object {

        private fun fetchCommand(outputDir: Path, markerDir: Path, classifier: String?): List<String> {
            val base = listOf(
                    "mvn",
                    "dependency:copy-dependencies",
                    "-DmarkersDirectory=${markerDir.toAbsolutePath().toFile()}",
                    "-DoutputDirectory=${outputDir.toAbsolutePath().toFile()}",
                    "-Dmdep.prependGroupId"
            )
            return if (classifier == null) {
                base
            } else {
                base + "-Dclassifier=$classifier"
            }
        }

    }

    private val log = logger()

    private fun <R> onFetchedArtifacts(
            artifacts: Collection<ArtifactId>,
            classifier: String?,
            transform: (files: Map<ArtifactId, Path>) -> R?
    ): R? {
        if (artifacts.isEmpty()) {
            return transform(emptyMap())
        }

        val combined = IoUtil.tempDir(prefix = "combined-", directory = basePath)
        val combinedFile = combined.toFile()
        return try {
            val project = IoUtil.tempDir(prefix = "project-", directory = combined).toFile()
            val repo = IoUtil.tempDir(prefix = "repo-", directory = combined)
            val markers = IoUtil.tempDir(prefix = "markers-", directory = combined)

            val processOuts = ProcessBuilder.Redirect.to(combinedFile.resolve("mvn-output.txt"))

            project.resolve("pom.xml").outputStream().use { out ->
                MockPomHandler.createMockPom(artifacts, out)
            }

            val copy = fetchCommand(repo, markers, classifier)
            val fileMap = IoUtil.runCommand(copy, project, processOuts) {
                log.debug("External maven dependency fetch process finished")

                artifacts.map {
                    it to repo.resolve(DependencyUtil.shortFilenameForArtifact(it, classifier) + ".jar")
                }.filter { (id, file) ->
                    id in artifacts && Files.exists(file)
                }.toMap()
            } ?: return null

            transform(fileMap)

        } catch (ioe: IOException) {
            log.error("IOException while fetching artifacts", ioe)
            null
        } finally {
            combinedFile.deleteRecursively()
        }
    }

    override fun fetchArtifacts(artifactIds: List<ArtifactId>, depTpResolver: TypeParamResolver): List<ArtifactInfoResult>? {
        log.logTrace { "Fetching artifacts - $artifactIds" }

        return onFetchedArtifacts(artifactIds, classifier = null) { artMap ->
            val infos = artMap.map { (_, path) ->
                val jarIn = JarInfo.single(path)
                val typeInfo = jarFileInfoCollector.collectTypeInfo(jarIn)

                jarIn to typeInfo
            }

            val fullTpResolver = CombinedTypeParamResolver(
                    infos.map { (_, ti) -> TypeInfoTypeParamResolver(ti.templateInfos) } + depTpResolver
            )

            infos.map { (jarIn, typeInfo) ->
                val funInfo = jarFunctionInfoCollector.collectFunctions(jarIn, fullTpResolver)

                ArtifactInfoResult(typeInfo, funInfo)
            }
        }
    }

    override fun fetchDocs(artifactIds: List<ArtifactId>): List<FunDocMap>? {
        log.logTrace { "Fetching artifact documentations for $artifactIds" }

        return onFetchedArtifacts(artifactIds, classifier = "javadoc") {
            it.map { (_, path) ->
                javadocParser.parseInput(path) ?: FunDocMap.empty()
            }
        }
    }

}