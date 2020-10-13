package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.parser.service.InfoCollector
import com.mktiti.fsearch.parser.util.JavaTypeParseLog

interface JarTypeCollector<I> {

    fun collectArtifact(info: I, javaRepo: JavaRepo, depsRepos: Collection<TypeRepo>): TypeRepo

}

class IndirectJarTypeCollector(
        private val infoRepo: JavaInfoRepo
) : JarTypeCollector<JarFileInfoCollector.JarInfo>, JclCollector<JarFileInfoCollector.JarInfo> {

    private val infoCollector = JarFileInfoCollector(infoRepo)

    override fun collectArtifact(info: JarFileInfoCollector.JarInfo, javaRepo: JavaRepo, depsRepos: Collection<TypeRepo>): TypeRepo {
        val (directs, templates) = infoCollector.collectInitial(info)
        return RadixTypeRepo(
                javaRepo = javaRepo,
                directs = directs,
                templates = templates
        )
    }

    override fun collectJcl(name: String, info: JarFileInfoCollector.JarInfo): JclCollector.JclResult {
        val (directs, templates) = infoCollector.collectInitial(info)

        templates[infoRepo.arrayType.packageName, infoRepo.arrayType.simpleName] = TypeTemplate(
                info = infoRepo.arrayType,
                superTypes = listOf(infoRepo.objectType.complete()),
                typeParams = listOf(TypeParameter("X", TypeBounds(emptySet()))),
                samType = null
        )
        PrimitiveType.values().map(infoRepo::primitive).forEach { primitive ->
            directs[primitive.packageName, primitive.simpleName] = DirectType(
                    minInfo = primitive,
                    superTypes = emptyList(),
                    virtual = false,
                    samType = null
            )
        }

        val javaRepo = RadixJavaRepo(
                artifact = name,
                directs = directs,
                infoRepo = infoRepo
        )

        val typeRepo = RadixTypeRepo(
                javaRepo = javaRepo,
                directs = directs,
                templates = templates
        )

        return JclCollector.JclResult(
                javaRepo = javaRepo,
                typeRepo = typeRepo
        )
    }

}

interface JclCollector<I> {

    data class JclResult(val javaRepo: JavaRepo, val typeRepo: TypeRepo)

    fun collectJcl(name: String, info: I): JclResult

}

class TwoPhaseCollector<I>(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog,
        private val infoCollector: InfoCollector<I>,
        private val connector: TypeConnector // = JavaTypeConnector(infoRepo, log)
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
