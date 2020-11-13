package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.ArtifactRepo
import com.mktiti.fsearch.maven.MavenArtifact
import com.mktiti.fsearch.maven.MavenRepoInfo
import java.io.*
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.*

class MavenManager(
        private val repo: MavenRepoInfo,
        private val dependencyResolver: DependencyResolver = PrimitiveDependencyResolver,
        private val localCache: LocalMavenRepoCache = LocalMavenRepoCache()
) : RepoArtifactFetch {

    companion object {
        val central = MavenRepoInfo(
                name = "Maven central",
                baseUrl = "https://repo1.maven.org/maven2/"
        )
    }

    private val repoLocation = createTempDir("fsearch-maven-repo-").apply {
        deleteOnExit()
    }

    private data class ArtifactStorage(
            val info: MavenArtifact,
            val dependencies: Collection<MavenArtifact>,
            val artifactRepo: ArtifactRepo
    )

    private val artifacts: MutableMap<MavenArtifact, ArtifactStorage> = Collections.synchronizedMap(mutableMapOf())

    private fun <T> downloadBase(info: MavenArtifact, ext: String, onChannel: (ReadableByteChannel) -> T): T? {
        val fileName = "${info.name}-${info.version}.$ext"
        val url = repo.baseUrl + (info.group + info.name + info.version + fileName).joinToString(separator = "/")

        println(">>> Downloading file $info from ${repo.name} ($url)")
        return try {
            Channels.newChannel(URL(url).openStream()).use(onChannel)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            return null
        }
    }


    private fun <T : Any> fetchDirect(info: MavenArtifact, ext: String, onStream: (InputStream) -> T?): T? {
        return localCache.onCachedFile(info, ext) { file ->
            println(">>> $info $ext found in local cache")
            FileInputStream(file).use(onStream)
        } ?: downloadBase(info, ext) { channel ->
            Channels.newInputStream(channel).use { stream ->
                onStream(stream)
            }
        }
    }

    private fun <T : Any> fetchFile(info: MavenArtifact, ext: String, onFile: (File) -> T?): T? {
        localCache.onCachedFile(info, ext, onFile)?.let {
            println(">>> $info $ext found in local cache")
            return it
        }

        val tempFile = createTempFile(prefix = "fsearch-maven-artifact-$ext-$info-", directory = repoLocation, suffix = ".$ext").apply {
            deleteOnExit()
        }

        return try {
            downloadBase(info, ext) { channel ->
                FileOutputStream(tempFile).use { fileOut ->
                    fileOut.channel.transferFrom(channel, 0, Long.MAX_VALUE)
                }
            }
            onFile(tempFile)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        } finally {
            tempFile.delete()
        }
    }

    private fun fetchArtifact(info: MavenArtifact, transform: (File) -> ArtifactRepo): ArtifactStorage? {
        return when (val stored = artifacts[info]) {
            null -> {
                val dependencies = fetchDirect(info, "pom") { pomStream ->
                    dependencyResolver.dependencies(pomStream)
                } ?: return null

                fetchFile(info, "jar", transform)?.let { repo ->
                    ArtifactStorage(info, dependencies, repo)
                }?.also {
                    artifacts[info] = it
                } ?: return null
            }
            else -> stored
        }
    }

    override fun fetchArtifactWithDeps(info: MavenArtifact, transform: (MavenArtifact, File) -> ArtifactRepo): Map<MavenArtifact, ArtifactRepo>? {
        val (_, dependencies, repo) = fetchArtifact(info) { file ->
            transform(info, file)
        } ?: return null

        println("Dependencies for $info: $dependencies")
        return dependencies.mapNotNull {
            fetchArtifactWithDeps(it, transform)
        }.fold(mutableMapOf<MavenArtifact, ArtifactRepo>()) { acc, map ->
            acc.apply {
                putAll(map)
            }
        }.apply {
            this[info] = repo
        }
    }

}
