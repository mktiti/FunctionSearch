package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.maven.util.DependencyUtil
import com.mktiti.fsearch.maven.util.IoUtil
import com.mktiti.fsearch.maven.util.JarHtmlJavadocParser
import com.mktiti.fsearch.maven.util.MockPomHandler
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.model.build.service.ArtifactTypeInfoCollector
import com.mktiti.fsearch.model.build.service.FunctionInfoCollector
import com.mktiti.fsearch.model.build.service.TypeInfoTypeParamResolver
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactInfoFetcher
import com.mktiti.fsearch.parser.function.JarFileFunctionInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ExternalMavenFetcher(
        private val basePath: Path = IoUtil.tempDir("fsearch-external-maven-fetch-"),
        private val infoRepo: JavaInfoRepo,
        private val jarFileInfoCollector: ArtifactTypeInfoCollector<JarInfo> = JarFileInfoCollector(infoRepo),
        private val jarFunctionInfoCollector: FunctionInfoCollector<JarInfo> = JarFileFunctionInfoCollector(infoRepo),
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

    private val docParser = JarHtmlJavadocParser(infoRepo)

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
                println(">>> External maven dependency fetch process finished")

                artifacts.map {
                    it to repo.resolve(DependencyUtil.shortFilenameForArtifact(it, classifier) + ".jar")
                }.filter { (id, file) ->
                    id in artifacts && Files.exists(file)
                }.toMap()
            } ?: return null

            transform(fileMap)

        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        } finally {
            combinedFile.deleteRecursively()
        }
    }

    override fun fetchArtifacts(artifactIds: List<ArtifactId>): List<ArtifactInfoResult>? {
        return onFetchedArtifacts(artifactIds, classifier = null) {
            it.map { (_, path) ->
                val jarIn = JarInfo.single(path)
                val typeInfo = jarFileInfoCollector.collectTypeInfo(jarIn)
                val typeParamResolver = TypeInfoTypeParamResolver(typeInfo.templateInfos)
                val funInfo = jarFunctionInfoCollector.collectFunctions(jarIn, typeParamResolver)

                ArtifactInfoResult(typeInfo, funInfo)
            }
        }
    }

    override fun fetchDocs(artifactIds: List<ArtifactId>): List<FunDocMap>? {
        return onFetchedArtifacts(artifactIds, classifier = "javadoc") {
            it.map { (_, path) ->
                docParser.parseJar(path.toFile()) ?: FunDocMap.empty()
            }
        }
    }

}