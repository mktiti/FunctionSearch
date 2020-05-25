package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.repo.JavaRepo
import com.mktiti.fsearch.core.repo.TypeRepo
import com.mktiti.fsearch.parser.service.InfoCollector
import com.mktiti.fsearch.parser.util.JavaTypeParseLog

interface JarTypeCollector<I> {

    fun collectArtifact(info: I, javaRepo: JavaRepo, depsRepos: Collection<TypeRepo>): TypeRepo

}

interface JclCollector<I> {

    data class JclResult(val javaRepo: JavaRepo, val typeRepo: TypeRepo)

    fun collectJcl(name: String, info: I): JclResult

}

class TwoPhaseCollector<I>(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog,
        private val infoCollector: InfoCollector<I>,
        private val connector: TypeConnector = JavaTypeConnector(infoRepo, log)
) : JarTypeCollector<I>, JclCollector<I> {

    override fun collectJcl(name: String, info: I): JclCollector.JclResult {
        val (directs, templates) = infoCollector.collectInitial(info)
        return connector.connectJcl(directs, templates, name)
    }

    override fun collectArtifact(info: I, javaRepo: JavaRepo, depsRepos: Collection<TypeRepo>): TypeRepo {
        val (directs, templates) = infoCollector.collectInitial(info)
        return connector.connectArtifact(directs, templates, javaRepo, depsRepos)
    }

}
