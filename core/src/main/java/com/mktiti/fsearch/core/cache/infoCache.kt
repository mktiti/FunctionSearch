package com.mktiti.fsearch.core.cache

import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PackageName
import com.mktiti.fsearch.util.cache.InternCache
import com.mktiti.fsearch.util.cache.MapInternTrie
import com.mktiti.fsearch.util.cache.SelfMapIntern
import com.mktiti.fsearch.util.logDebug
import com.mktiti.fsearch.util.logger
import java.util.*
import java.util.concurrent.atomic.AtomicReference

interface InfoCache {

    fun minimalInfo(packageName: PackageName, simpleName: String, virtual: Boolean = false): MinimalInfo

    fun funInfo(functionInfo: FunctionInfo): FunctionInfo

    fun staticCompleteMinInfo(info: CompleteMinInfo.Static): CompleteMinInfo.Static

    fun dynamicCompleteMinInfo(info: CompleteMinInfo.Dynamic): CompleteMinInfo.Dynamic

}

interface CleanableInfoCache : InfoCache {

    private class NonCleanableWrapper(
            private val cache: InfoCache
    ) : InfoCache by cache, CleanableInfoCache {
        override fun clean() {}
    }

    companion object {
        fun wrap(cache: InfoCache) : CleanableInfoCache = NonCleanableWrapper(cache)

        val nop: CleanableInfoCache = wrap(NopInfoCache)
    }

    fun clean()

}

object NopInfoCache : InfoCache {

    override fun minimalInfo(packageName: PackageName, simpleName: String, virtual: Boolean)
        = MinimalInfo(packageName.map { it.intern() }, simpleName.intern(), virtual)

    override fun funInfo(functionInfo: FunctionInfo): FunctionInfo = functionInfo

    override fun staticCompleteMinInfo(info: CompleteMinInfo.Static): CompleteMinInfo.Static = info

    override fun dynamicCompleteMinInfo(info: CompleteMinInfo.Dynamic): CompleteMinInfo.Dynamic = info

}

class CleaningInternCache : CleanableInfoCache {

    private val packageCache: InternCache<PackageName> = MapInternTrie()
    private val infoCache: InternCache<MinimalInfo> = SelfMapIntern()

    private val funInfoCache: InternCache<FunctionInfo> = SelfMapIntern()

    private val cmiCache: InternCache<CompleteMinInfo.Static> = SelfMapIntern()
    private val dmiCache: InternCache<CompleteMinInfo.Dynamic> = SelfMapIntern()

    private val cleanableCaches = listOf(
            packageCache, infoCache, funInfoCache, cmiCache, dmiCache
    )

    private fun packageName(packageName: PackageName): PackageName {
        return packageCache.intern(LinkedList(packageName))
    }

    override fun minimalInfo(packageName: PackageName, simpleName: String, virtual: Boolean): MinimalInfo {
        return infoCache.intern(MinimalInfo(packageName(packageName), simpleName.intern(), virtual = false))
    }

    override fun funInfo(functionInfo: FunctionInfo): FunctionInfo {
        return funInfoCache.intern(functionInfo)
    }

    override fun staticCompleteMinInfo(info: CompleteMinInfo.Static): CompleteMinInfo.Static {
        return cmiCache.intern(info)
    }

    override fun dynamicCompleteMinInfo(info: CompleteMinInfo.Dynamic): CompleteMinInfo.Dynamic {
        return dmiCache.intern(info)
    }

    override fun clean() {
        cleanableCaches.forEach(InternCache<out Any>::clean)
    }

}

object CentralInfoCache : CleanableInfoCache {

    private val cache: AtomicReference<CleanableInfoCache> = AtomicReference(CleanableInfoCache.nop)

    private val log = logger()

    fun setCache(cache: InfoCache?) {
        setCleanableCache(CleanableInfoCache.wrap(cache ?: NopInfoCache))
        log.logDebug { "Central cache updated to ${cache?.javaClass?.canonicalName}" }
    }

    fun setCleanableCache(cache: CleanableInfoCache?) {
        this.cache.set(cache ?: CleanableInfoCache.nop)
    }

    override fun minimalInfo(packageName: PackageName, simpleName: String, virtual: Boolean): MinimalInfo
        = cache.get().minimalInfo(packageName, simpleName, virtual)

    override fun funInfo(functionInfo: FunctionInfo): FunctionInfo
        = cache.get().funInfo(functionInfo)

    override fun staticCompleteMinInfo(info: CompleteMinInfo.Static): CompleteMinInfo.Static
        = cache.get().staticCompleteMinInfo(info)

    override fun dynamicCompleteMinInfo(info: CompleteMinInfo.Dynamic): CompleteMinInfo.Dynamic
        = cache.get().dynamicCompleteMinInfo(info)

    override fun clean() {
        cache.get().clean()
        log.info("CentralInfoCache cleaned")
    }

}