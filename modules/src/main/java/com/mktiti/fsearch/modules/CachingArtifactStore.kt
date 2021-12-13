package com.mktiti.fsearch.modules

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.mktiti.fsearch.model.build.intermediate.ArtifactInfoResult
import com.mktiti.fsearch.model.build.intermediate.FunDocMap
import com.mktiti.fsearch.modules.store.GenericArtifactStore
import java.util.concurrent.ExecutionException

class CachingArtifactStore<T>(
        private val backingStore: GenericArtifactStore<T>,
        maxWeight: Long,
        weightMeasure: (data: T) -> Int
) : GenericArtifactStore<T> {

    private object NotFoundException : Exception()

    companion object {
        fun cachedDocsStore(
                backingStore: GenericArtifactStore<FunDocMap>,
                maxWeight: Long
        ) = CachingArtifactStore(backingStore, maxWeight) { data ->
            (data.map.sumOf { (_, doc) ->
                (doc.shortInfo?.length?.toLong() ?: 0L) + (doc.longInfo?.length?.toLong() ?: 0L)
            } / 1500L).toInt()
        }

        fun cachedInfoStore(
                backingStore: GenericArtifactStore<ArtifactInfoResult>,
                maxWeight: Long
        ) = CachingArtifactStore(backingStore, maxWeight) { data ->
            val kbSum: Double =
                    (data.funInfo.instanceMethods.size.toDouble() * 5.5) +
                    (data.funInfo.staticFunctions.size.toDouble() * 0.75) +
                    (data.typeInfo.templateInfos.size.toDouble() * 0.7) +
                    (data.typeInfo.directInfos.size * 0.33)

            kbSum.toInt()
        }

        fun cachedDepsStore(
                backingStore: GenericArtifactStore<List<ArtifactId>>,
                maxWeight: Long
        ) = CachingArtifactStore(backingStore, maxWeight) { data ->
            data.size / 10
        }

        private fun <T> loader(backingStore: GenericArtifactStore<T>): CacheLoader<ArtifactId, T> = CacheLoader.from { input ->
            input?.let(backingStore::getData) ?: throw NotFoundException
        }
    }

    private val cache: LoadingCache<ArtifactId, T> = CacheBuilder.newBuilder()
            .maximumWeight(maxWeight)
            .weigher { _: ArtifactId, value: T -> weightMeasure(value) }
            .build(loader(backingStore))

    override fun store(artifact: ArtifactId, data: T) {
        backingStore.store(artifact, data)
        cache.put(artifact, data)
    }

    override fun getData(artifact: ArtifactId): T? = try {
        cache.get(artifact)
    } catch (exception: ExecutionException) {
        null
    }

    override fun remove(artifact: ArtifactId) {
        backingStore.remove(artifact)
        cache.invalidate(artifact)
    }

    override fun allStored(): Set<ArtifactId> = backingStore.allStored()

}
