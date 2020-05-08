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
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.mapMutablePrefixTree

interface TypeConnector {

    fun connectJcl(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            jclArtifact: String
    ): JclResult

    fun connectArtifact(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            javaRepo: JavaRepo
    ): TypeRepo

}

class JavaTypeConnector(private val infoRepo: JavaInfoRepo) : TypeConnector {

    private fun <T> useOneshot(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            code: OneshotConnector.() -> T
    ) = OneshotConnector(infoRepo, imDirectTypes, imTemplateTypes).code()

    override fun connectJcl(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            jclArtifact: String
    ): JclResult = useOneshot(imDirectTypes, imTemplateTypes) {
        connectJcl(jclArtifact)
    }

    override fun connectArtifact(
            imDirectTypes: MutablePrefixTree<String, DirectCreator>,
            imTemplateTypes: MutablePrefixTree<String, TemplateCreator>,
            javaRepo: JavaRepo
    ): TypeRepo = useOneshot(imDirectTypes, imTemplateTypes) {
        connectArtifact(javaRepo)
    }

}

private class OneshotConnector(
        private val infoRepo: JavaInfoRepo,
        private val imDirectTypes: MutablePrefixTree<String, DirectCreator>,
        private val imTemplateTypes: MutablePrefixTree<String, TemplateCreator>
) {

    private val directTypes: MutablePrefixTree<String, DirectType> = mapMutablePrefixTree()
    private val typeTemplates: MutablePrefixTree<String, TypeTemplate> = mapMutablePrefixTree()

    private fun anyDirect(info: MinimalInfo): DirectType? {
        return directTypes[info] ?: imDirectTypes[info]?.unfinishedType
    }

    private fun anyTemplate(info: MinimalInfo): TypeTemplate? {
        return typeTemplates[info] ?: imTemplateTypes[info]?.unfinishedType
    }

    fun connectArtifact(javaRepo: JavaRepo): TypeRepo {
        connect()

        return RadixTypeRepo(
                javaRepo = javaRepo,
                directs = directTypes,
                templates = typeTemplates
        )
    }

    fun connectJcl(jclArtifact: String): JclResult {
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

        return JclResult(javaRepo, jclTypeRepo)
    }

    private fun connect() {
        println("==== Resolving direct super types")
        resolveDirectSupers(imDirectTypes)
        resolveDirectSupers(imTemplateTypes)

        var iter = 0
        while (!imDirectTypes.empty) {
            var removeCount = 0
            println("=== Iter #${iter++} -- direct types (${imDirectTypes.size}), type templates (${imTemplateTypes.size}):")
            // imDirectTypes.forEach { direct ->
            //   println(direct.unfinishedType)
            // }

            println("  = Resolving generic super types")
            resolveTemplateSupers(imDirectTypes)
            resolveTemplateSupers(imTemplateTypes)

            imDirectTypes.removeIf { direct ->
                (direct.templateSupers.isEmpty()).also { done ->
                    if (done) {
                        val type = direct.unfinishedType
                        val info = type.info
                        // directTypes.mutableSubtreeSafe(com.mktiti.fsearch.core.type.info.packageName)[com.mktiti.fsearch.core.type.info.name] = com.mktiti.fsearch.parser.type
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
                        // directTypes.mutableSubtreeSafe(com.mktiti.fsearch.core.type.info.packageName)[com.mktiti.fsearch.core.type.info.name] = com.mktiti.fsearch.parser.type
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

    private fun <T : SemiType> resolveDirectSupers(types: PrefixTree<String, TypeCreator<T>>) {
        types.forEach { creator ->
            creator.directSupers.forEach { directSuper ->
                val superType = anyDirect(directSuper)

                if (superType != null) {
                    creator.addNonGenericSuper(superType)
                } else {
                    when (val superTemplate = anyTemplate(directSuper)) {
                        null -> {
                            println("Super type $directSuper not found (possibly not public), should create stub type")
                        }
                        else -> {
                            println("Raw use of super type ${superTemplate.fullName} for type ${creator.unfinishedType} parameterized with ${infoRepo.objectType}")
                            creator.templateSupers += DatCreator(
                                    template = directSuper,
                                    args = superTemplate.typeParams.map {
                                        TypeArgCreator.Direct(infoRepo.objectType)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun mapTypeArgCreator(creator: TypeArgCreator): ApplicationParameter? {
        return when (creator) {
            TypeArgCreator.Wildcard -> Direct
            is TypeArgCreator.UpperWildcard -> {
                val bound: Substitution = mapTypeArgCreator(creator.bound) as? Substitution ?: return null
                BoundedWildcard.UpperBound(bound)
            }
            is TypeArgCreator.Direct -> {
                val type = anyDirect(creator.arg) ?: return null
                StaticTypeSubstitution(type)
            }
            is TypeArgCreator.Dat -> {
                val type = (mapDatCreator(creator.dat, false) as? DatResult.Success)?.type ?: return null
                TypeSubstitution.wrap(type)
            }
            is TypeArgCreator.Param -> ParamSubstitution(creator.sign)
        }
    }

    private sealed class DatResult {
        object Error : DatResult()
        object NotReady : DatResult()
        data class Success(val type: Type) : DatResult()
    }

    private fun mapDatCreator(creator: DatCreator, onlyUseReady: Boolean): DatResult {
        val info = creator.template
        val template: TypeTemplate = if (onlyUseReady) {
            when (val ready = typeTemplates[info]) {
                null -> return if (anyTemplate(info) != null) DatResult.NotReady else DatResult.Error
                else -> ready
            }
        } else {
            anyTemplate(info) ?: return DatResult.Error
        }

        val mappedArgs = creator.args.map { mapTypeArgCreator(it) }.liftNull() ?: return DatResult.Error
        return DatResult.Success(template.apply(mappedArgs) ?: return DatResult.Error)
    }

    private fun <T : SemiType> resolveTemplateSupers(types: PrefixTree<String, TypeCreator<T>>) {
        types.forEach { creator ->
            creator.templateSupers.removeIf { superCreator ->
                when (val superResult = mapDatCreator(superCreator, true)) {
                    DatResult.Error -> {
                        println("Generic super type ${creator.unfinishedType} could not be applied properly, is skipped")
                        true
                    }
                    DatResult.NotReady -> false
                    is DatResult.Success -> {
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
