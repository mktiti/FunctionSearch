package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.util.InfoMap
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.modules.ArtifactManager
import com.mktiti.fsearch.modules.DomainRepo
import com.mktiti.fsearch.modules.SimpleDomainRepo
import com.mktiti.fsearch.parser.intermediate.FunctionCollector
import com.mktiti.fsearch.parser.intermediate.IndirectInfoCollector
import com.mktiti.fsearch.parser.intermediate.type.JarFileInfoCollector
import com.mktiti.fsearch.util.orElse
import java.io.File
import java.util.stream.Collectors

// TODO proper domain caching
class MavenMapArtifactManager(
        private val typeCollector: IndirectInfoCollector<JarFileInfoCollector.JarInfo>,
        private val funCollector: FunctionCollector<JarFileInfoCollector.JarInfo>,
        private val infoRepo: JavaInfoRepo,
        private val javaRepo: JavaRepo,
        private val baseResolver: TypeResolver,
        private val mavenFetcher: MavenFetcher
) : ArtifactManager {

    companion object {
        private fun jarInfo(artifact: ArtifactId, file: File) = JarFileInfoCollector.JarInfo(
                name = artifact.toString(),
                paths = listOf(file.toPath())
        )
    }

    private val stored = mutableMapOf<ArtifactId, DomainRepo>()

    override fun allStored(): Set<ArtifactId> = stored.keys

    override fun effective(artifacts: Collection<ArtifactId>) = mavenFetcher.runOnArtifactWithDeps(artifacts) { files ->
        files.keys
    } ?: error("Failed to resolve dependencies")

    private fun processFile(artifact: ArtifactId, jarFile: File): DomainRepo {
        return stored.getOrPut(artifact) {
            val jarInfo = jarInfo(artifact, jarFile)

            val (directs, templates) = typeCollector.collectInitial(jarInfo)
            val typeRepo = MapTypeRepo(directs, templates)
            val functions = funCollector.collectFunctions(jarInfo, javaRepo, infoRepo, baseResolver)

            SimpleDomainRepo(
                    typeResolver = SingleRepoTypeResolver(typeRepo),
                    functions = functions
            )
        }
    }

    private fun fetchSingle(artifact: ArtifactId): DomainRepo = mavenFetcher.runOnArtifact(artifact) { jar ->
        processFile(artifact, jar)
    } ?: error("Failed to load $artifact")

    override fun getSingle(artifact: ArtifactId): DomainRepo {
        return stored[artifact].orElse {
            fetchSingle(artifact)
        }
    }

    private fun fetchWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo = mavenFetcher.runOnArtifactWithDeps(artifacts) { files ->
        val entries = files.entries.map { (artifact, file) ->
            artifact to jarInfo(artifact, file)
        }

        val newResolvers = entries.map { (artifact, jarInfo) ->
            stored[artifact]?.typeResolver.orElse {
                val (directs, templates) = typeCollector.collectInitial(jarInfo)
                val typeRepo = MapTypeRepo(directs, templates)

                SingleRepoTypeResolver(typeRepo)
            }
        }
        val resolver = SimpleCombiningTypeResolver(newResolvers)
        val processResolver = FallbackResolver(SimpleCombiningTypeResolver(newResolvers), baseResolver)

        val (statics, instances) = entries.map { (_, jarInfo) ->
            funCollector.collectFunctions(jarInfo, javaRepo, infoRepo, processResolver).let { (statics, instances) ->
                statics to instances
            }
        }.unzip()

        val combined = InfoMap.combine(instances) { results -> results.flatten() }

        SimpleDomainRepo(
                typeResolver = resolver,
                staticStore = statics.flatten(),
                instanceFunctions = combined
        )
    } ?: error("Failed to load $artifacts")

    override fun getWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo {
        return artifacts.map { stored[it] }.liftNull()?.let { domains ->
            val resolver = SimpleCombiningTypeResolver(domains.map { it.typeResolver })

            val (statics, instances) = domains.map {
                it.staticFunctions to it.instanceFunctions
            }.unzip()

            val stats = statics.flatMap { it.collect(Collectors.toList()) }
            val combined = InfoMap.combine(instances) { results -> results.flatten() }

            SimpleDomainRepo(
                    typeResolver = resolver,
                    staticStore = stats,
                    instanceFunctions = combined
            )
        }.orElse {
            fetchWithDependencies(artifacts)
        }
    }

    override fun remove(artifact: ArtifactId): Boolean = stored.remove(artifact) != null

}