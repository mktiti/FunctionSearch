package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Direct
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.util.JavaTypeParseLog
import com.mktiti.fsearch.parser.util.anyDirect
import com.mktiti.fsearch.parser.util.anyTemplate
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.mapMutablePrefixTree

interface TypeConnector {

    fun connectJcl(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            jclArtifact: String
    ): JclCollector.JclResult

    fun connectArtifact(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            javaRepo: JavaRepo,
            depsRepos: Collection<TypeRepo>
    ): TypeRepo

}

class JavaTypeConnector(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog
) : TypeConnector {

    private fun <T> useOneshot(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            depsRepos: Collection<TypeRepo>,
            code: OneshotConnector.() -> T
    ) = OneshotConnector(log, infoRepo, depsRepos, imDirectTypes, imTemplateTypes).code()

    override fun connectJcl(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            jclArtifact: String
    ): JclCollector.JclResult = useOneshot(imDirectTypes, imTemplateTypes, emptyList()) {
        connectJcl(jclArtifact)
    }

    override fun connectArtifact(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            javaRepo: JavaRepo,
            depsRepos: Collection<TypeRepo>
    ): TypeRepo = useOneshot(imDirectTypes, imTemplateTypes, depsRepos) {
        connectArtifact(javaRepo)
    }

}

private class OneshotConnector(
        private val log: JavaTypeParseLog,
        private val infoRepo: JavaInfoRepo,
        private val depsRepos: Collection<TypeRepo>,
        private val imDirectTypes: MutablePrefixTree<String, DirectCreator>,
        private val imTemplateTypes: MutablePrefixTree<String, TemplateCreator>
) {

    private val directTypes: MutablePrefixTree<String, DirectType> = mapMutablePrefixTree()
    private val typeTemplates: MutablePrefixTree<String, TypeTemplate> = mapMutablePrefixTree()

    private fun anyDirect(info: MinimalInfo): DirectType? {
        return directTypes[info] ?: imDirectTypes[info]?.unfinishedType ?: depsRepos.anyDirect(info)
    }

    private fun readyTemplate(info: MinimalInfo): TypeTemplate? {
        return typeTemplates[info] ?: depsRepos.anyTemplate(info)
    }

    private fun anyTemplate(info: MinimalInfo): TypeTemplate? {
        return readyTemplate(info) ?: imTemplateTypes[info]?.unfinishedType
    }

    fun connectArtifact(javaRepo: JavaRepo): TypeRepo {
        connect()

        return RadixTypeRepo(
                javaRepo = javaRepo,
                directs = directTypes,
                templates = typeTemplates
        )
    }

    fun connectJcl(jclArtifact: String): JclCollector.JclResult {
        val arraySupers = ArrayList<SuperType.StaticSuper>(1)
        val arrayTemplate = TypeTemplate(
                info = infoRepo.arrayType.full(jclArtifact),
                superTypes = arraySupers,
                typeParams = listOf(TypeParameter("X", TypeBounds(emptySet())))
        )
        imTemplateTypes[infoRepo.arrayType.packageName, infoRepo.arrayType.simpleName] = TemplateCreator(
                unfinishedType = arrayTemplate,
                directSupers = listOf(infoRepo.objectType),
                templateSupers = mutableListOf(),
                directSuperAppender = { arraySupers += SuperType.StaticSuper.EagerStatic(it) },
                templateSuperAppender = {}
        )
        PrimitiveType.values().map(infoRepo::primitive).forEach { primitive ->
            directTypes[primitive.packageName, primitive.simpleName] = DirectType(primitive.full(jclArtifact), emptyList())
        }

        connect()

        val javaRepo = RadixJavaRepo(
                artifact = jclArtifact,
                infoRepo = infoRepo,
                directs = directTypes
        )

        val jclTypeRepo = RadixTypeRepo(
                javaRepo = javaRepo,
                directs = directTypes,
                templates = typeTemplates
        )

        return JclCollector.JclResult(javaRepo, jclTypeRepo)
    }

    private fun connect() {
        resolveDirectSupers(imDirectTypes)
        resolveDirectSupers(imTemplateTypes)

        while (!imDirectTypes.empty || !imTemplateTypes.empty) {
            var removeCount = 0
            resolveTemplateSupers(imDirectTypes)
            resolveTemplateSupers(imTemplateTypes)

            imDirectTypes.removeIf { direct ->
                (direct.templateSupers.isEmpty()).also { done ->
                    if (done) {
                        val type = direct.unfinishedType
                        val info = type.info
                        // directTypes.mutableSubtreeSafe(info.packageName)[info.name] = type
                        directTypes[info.packageName, info.name] = type
                        removeCount++
                    }
                }
            }

            imTemplateTypes.removeIf { template ->
                (template.templateSupers.isEmpty()).also { done ->
                    if (done) {
                        val type = template.unfinishedType
                        val info = type.info
                        // directTypes.mutableSubtreeSafe(info.packageName)[info.name] = type
                        typeTemplates[info.packageName, info.name] = type
                        removeCount++
                    }
                }
            }

            if (removeCount == 0) {
                println("Still present ==========>")
                imDirectTypes.forEach { direct ->
                       println(direct.unfinishedType)
                }
                break
                // error("Failed to remove during iteration")
            }
        }
    }

    private fun rawUse(template: TypeTemplate) = DatCreator(
            template = MinimalInfo.fromFull(template.info),
            args = template.typeParams.map {
                TypeArgCreator.Direct(infoRepo.objectType)
            }
    )

    private fun <T : SemiType> resolveDirectSupers(types: PrefixTree<String, TypeCreator<T>>) {
        types.forEach { creator ->
            creator.directSupers.forEach { directSuper ->
                val superType = anyDirect(directSuper)

                if (superType != null) {
                    creator.addNonGenericSuper(superType)
                } else {
                    when (val superTemplate = anyTemplate(directSuper)) {
                        null -> {
                            log.typeNotFound(creator.unfinishedType.info, directSuper)
                        }
                        else -> {
                            log.rawUsage(creator.unfinishedType.info, directSuper)
                            creator.templateSupers += rawUse(superTemplate)
                        }
                    }
                }
            }
        }
    }

    private fun mapTypeArgCreator(user: TypeInfo, creator: TypeArgCreator): CreationResult<ApplicationParameter> {
        fun success(param: ApplicationParameter) = CreationResult.Success(param)
        fun notFound(info: MinimalInfo) = CreationResult.Error.NotFound<ApplicationParameter>(info)
        fun appError() = CreationResult.Error.Application<ApplicationParameter>()

        fun dat(creator: DatCreator): CreationResult<ApplicationParameter> {
            val type = (mapDatCreator(creator, false) as? CreationResult.Success)?.type ?: return appError()
            return success(TypeSubstitution.wrap(type))
        }

        return when (creator) {
            TypeArgCreator.Wildcard -> success(Direct)
            is TypeArgCreator.UpperWildcard -> {
                val nested = mapTypeArgCreator(user, creator.bound)
                if (nested is CreationResult.Success) {
                    val bound: Substitution = nested.type as? Substitution ?: return appError()
                    success(BoundedWildcard.UpperBound(bound))
                } else {
                    nested
                }
            }
            is TypeArgCreator.Direct -> {
                val direct = anyDirect(creator.arg)
                if (direct != null) {
                    success(StaticTypeSubstitution(direct))
                } else {
                    anyTemplate(creator.arg)?.let {
                        log.rawUsage(used = creator.arg, user = user)
                        dat(rawUse(it))
                    } ?: notFound(creator.arg)
                }
            }
            is TypeArgCreator.Dat -> dat(creator.dat)
            is TypeArgCreator.Param -> success(ParamSubstitution(creator.sign))
        }
    }

    private sealed class CreationResult<T> {
        class NotReady<T> : CreationResult<T>()
        sealed class Error<T> : CreationResult<T>() {
            data class NotFound<T>(val info: MinimalInfo) : CreationResult.Error<T>()
            class Application<T> : CreationResult.Error<T>()
        }
        data class Success<T>(val type: T) : CreationResult<T>()
    }

    private fun mapDatCreator(creator: DatCreator, onlyUseReady: Boolean): CreationResult<Type> {
        val info = creator.template
        val template: TypeTemplate = if (onlyUseReady) {
            when (val ready = readyTemplate(info)) {
                null -> return if (anyTemplate(info) != null) {
                    CreationResult.NotReady()
                } else {
                    CreationResult.Error.NotFound(info)
                }
                else -> ready
            }
        } else {
            anyTemplate(info) ?: return CreationResult.Error.NotFound(info)
        }

        fun applicationError() = CreationResult.Error.Application<Type>()

        val mappedArgs = creator.args
                .map {
                    when (val mapped = mapTypeArgCreator(creator.template.full(""), it)) {
                        is CreationResult.NotReady -> return CreationResult.NotReady()
                        is CreationResult.Error.NotFound -> return CreationResult.Error.NotFound(mapped.info)
                        is CreationResult.Error.Application -> return CreationResult.Error.Application()
                        is CreationResult.Success -> mapped.type
                    }
                }
        return CreationResult.Success(template.apply(mappedArgs) ?: return applicationError())
    }

    private fun <T : SemiType> resolveTemplateSupers(types: PrefixTree<String, TypeCreator<T>>) {
        types.forEach { creator ->
            creator.templateSupers.removeIf { superCreator ->
                when (val superResult = mapDatCreator(superCreator, true)) {
                    is CreationResult.NotReady -> false
                    is CreationResult.Error.NotFound -> {
                        log.typeNotFound(creator.unfinishedType.info, superResult.info)
                        true
                    }
                    is CreationResult.Error.Application -> {
                        log.applicationError(creator.unfinishedType.info, superCreator.template)
                        true
                    }
                    is CreationResult.Success -> {
                        val superType = superResult.type
                        when (creator) {
                            is DirectCreator -> {
                                when (superType) {
                                    is Type.DynamicAppliedType -> {
                                        println("Non-generic type ${creator.unfinishedType} has generic super type $superType")
                                    }
                                    is Type.NonGenericType -> {
                                        creator.addNonGenericSuper(superType)
                                    }
                                }
                            }
                            is TemplateCreator -> {
                                when (superType) {
                                    is Type.NonGenericType -> creator.addNonGenericSuper(superType)
                                    is Type.DynamicAppliedType -> creator.templateSuperAppender(superType)
                                }
                            }
                        }
                        true
                    }
                }
            }
        }
    }

}
