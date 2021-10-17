package com.mktiti.fsearch.modules

import arrow.core.flatten
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.core.repo.SimpleCombiningTypeResolver
import com.mktiti.fsearch.core.repo.SingleRepoTypeResolver
import com.mktiti.fsearch.core.util.InfoMap
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.service.FunctionConnector
import com.mktiti.fsearch.model.build.service.TypeInfoConnector
import com.mktiti.fsearch.model.build.service.TypeInfoTypeParamResolver
import com.mktiti.fsearch.parser.function.JarFileFunctionInfoCollector
import com.mktiti.fsearch.parser.parse.JarInfo
import com.mktiti.fsearch.parser.type.JarFileInfoCollector
import com.mktiti.fsearch.util.orElse
import com.mktiti.fsearch.util.splitMapKeep
import java.nio.file.Path

class SecondaryArtifactManager(
        private val typeInfoConnector: TypeInfoConnector,
        private val functionConnector: FunctionConnector,
        private val infoCache: ArtifactInfoStore,
        private val depInfoFetcher: DependencyInfoFetcher,
        private val artifactInfoFetcher: ArtifactInfoFetcher
) : ArtifactManager {

    private val jclMap = mutableMapOf<String, Pair<DomainRepo, JavaRepo>>()

    override fun allStored(): Set<ArtifactId> = emptySet()

    override fun effective(artifacts: Collection<ArtifactId>): Set<ArtifactId> = emptySet()

    override fun getOrLoadJcl(version: String, paths: Collection<Path>): Pair<DomainRepo, JavaRepo> {
        return jclMap.getOrPut(version) {
            val (typeInfo, funInfo) = infoCache.getOrStore(ArtifactId.jcl(version)) {
                val jclJarInfo = JarInfo("JCL", paths)

                val jarTypeLoader = JarFileInfoCollector(MapJavaInfoRepo)
                val jarFunLoader = JarFileFunctionInfoCollector(MapJavaInfoRepo)


                val rawTypeInfo = jarTypeLoader.collectTypeInfo(jclJarInfo)
                val typeParamResolver = TypeInfoTypeParamResolver(rawTypeInfo.templateInfos)

                val rawFunInfo = jarFunLoader.collectFunctions(jclJarInfo, typeParamResolver)

                ArtifactInfoResult(rawTypeInfo, rawFunInfo)
            }

            val (javaRepo, jclRepo) = typeInfoConnector.connectJcl(typeInfo)
            val funs = functionConnector.connect(funInfo)

            val jclResolver = SingleRepoTypeResolver(jclRepo)

            SimpleDomainRepo(jclResolver, funs) to javaRepo
        }
    }

    override fun getSingle(artifact: ArtifactId): DomainRepo {
        val (typeInfo, funInfo) = infoCache[artifact].orElse {
            artifactInfoFetcher.fetchArtifact(artifact)?.also {
                infoCache.store(artifact, it)
            } ?: error("Failed to fetch dependencies")
        }

        return SimpleDomainRepo(
                typeInfoConnector.connectArtifact(typeInfo),
                functionConnector.connect(funInfo)
        )
    }

    override fun remove(artifact: ArtifactId): Boolean = false

    override fun getWithDependencies(artifacts: Collection<ArtifactId>): DomainRepo {
        val artifactDepGraph = depInfoFetcher.getDependencies(artifacts) ?: error("Failed to fetch dependencies")

        val (artifactInfos, missingArtifacts) = artifactDepGraph.keys.splitMapKeep {
            infoCache[it]
        }

        val allArtifacts: List<ArtifactInfoResult> = artifactInfos + if (missingArtifacts.isNotEmpty()) {
            artifactInfoFetcher.fetchArtifacts(missingArtifacts)?.also {
                missingArtifacts.zipIfSameLength(it)?.forEach { (id, artifact) ->
                    infoCache.store(id, artifact)
                }
            } ?: error("Failed to fetch dependencies")
        } else {
            emptyList()
        }

        val resolvers = allArtifacts.map {
            typeInfoConnector.connectArtifact(it.typeInfo)
        }
        val combinedResolver = SimpleCombiningTypeResolver(resolvers)

        val (statics, instances) = allArtifacts.map {
            functionConnector.connect(it.funInfo).asPair()
        }.unzip()
        val combinedInstanceFuns = InfoMap.combine(instances) { results -> results.flatten() }

        return SimpleDomainRepo(
                typeResolver = combinedResolver,
                staticStore = statics.flatten(),
                instanceFunctions = combinedInstanceFuns
        )
    }

}