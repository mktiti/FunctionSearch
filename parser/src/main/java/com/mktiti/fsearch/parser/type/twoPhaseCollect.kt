package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.util.MutablePrefixTree

typealias IndirectResults<T> = MutablePrefixTree<String, T>

data class TypeParamInfo(
        val sign: String,
        val bounds: List<CompleteMinInfo<*>>
)

sealed class CreatorInfo(
        val info: MinimalInfo,
        val directSupers: Collection<CompleteMinInfo.Static>
) {

    class Direct(
            info: MinimalInfo,
            directSupers: Collection<CompleteMinInfo.Static>
    ) : CreatorInfo(info, directSupers)

    class Template(
            info: MinimalInfo,
            val typeParams: List<TypeParamInfo>,
            directSupers: Collection<CompleteMinInfo.Static>,
            val datSupers: Collection<CompleteMinInfo.Dynamic>
    ) : CreatorInfo(info, directSupers)

}
/*
class TwoPhaseCollector<in I>(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog,
        private val infoCollector: InfoCollector<I>,
        private val connector: TypeConnector
) : JarTypeCollector<I>, JclCollector<I> {

    data class InitialData(
            val directs: IndirectResults<CreatorInfo.Direct>,
            val templates: IndirectResults<CreatorInfo.Template>
    )

    interface InfoCollector<in I> {
        fun collectInfo(info: I, infoRepo: JavaInfoRepo): InitialData
    }

    override fun collectJcl(info: I, name: String): JclCollector.Result {
        val (directs, templates) = infoCollector.collectInfo(info, infoRepo)
        return connector.connectJcl(directs, templates, name)
    }

    override fun collectArtifact(info: I, javaRepo: JavaRepo, dependencyResolver: TypeResolver): TypeRepo {
        val (directs, templates) = infoCollector.collectInfo(info, infoRepo)
        return connector.connectArtifact(directs, templates, javaRepo, dependencyResolver)
    }

}
 */