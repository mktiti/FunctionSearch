package com.mktiti.fsearch.model.connect.type

import com.mktiti.fsearch.core.cache.InfoCache
import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.model.build.intermediate.*
import com.mktiti.fsearch.model.build.service.JclTypeResult
import com.mktiti.fsearch.model.build.service.TypeInfoConnector
import com.mktiti.fsearch.model.build.util.JavaTypeParseLog
import com.mktiti.fsearch.util.orElse

class JavaTypeInfoConnector(
        private val infoRepo: JavaInfoRepo,
        private val internCache: InfoCache,
        private val log: JavaTypeParseLog
) : TypeInfoConnector {

    private fun <T> useOneshot(
            rawInfoResults: TypeInfoResult,
            code: OneshotConnector.() -> T
    ) = OneshotConnector(infoRepo, internCache, rawInfoResults).code()

    override fun connectJcl(infoResults: TypeInfoResult) = useOneshot(infoResults) {
        connectJcl()
    }

    override fun connectArtifact(infoResults: TypeInfoResult) = useOneshot(infoResults) {
        connectArtifact()
    }

}

private sealed class SemiResult<out S : SemiType> {

    class NotFound<S : SemiType> : SemiResult<S>()

    data class Ready<S : SemiType>(val readySemi: S) : SemiResult<S>()

    data class Unready<S : SemiType>(val creator: SemiCreator<S>) : SemiResult<S>()

}

private class OneshotConnector(
        private val infoRepo: JavaInfoRepo,
        private val internCache: InfoCache,
        rawInfoResults: TypeInfoResult
) {

    private fun List<IntMinInfo>.infoMutableConverted() = map { it.toMinimalInfo(internCache) }.toMutableList()
    private fun List<IntStaticCmi>.cmiMutableConverted() = map { it.convert(internCache) }.toMutableList()

    private val directCreators: MutableMap<MinimalInfo, DirectCreator> = directCreators(rawInfoResults)
    private val templateCreators: MutableMap<MinimalInfo, TemplateCreator> = templateCreators(rawInfoResults)
    private val allCreators: Collection<SemiCreator<*>>
        get() = listOf(directCreators, templateCreators).flatMap { it.values }

    private val readyDirects: MutableMap<MinimalInfo, DirectType> = HashMap()
    private val readyTemplates: MutableMap<MinimalInfo, TypeTemplate> = HashMap()

    fun SemiInfo.DirectInfo.initCreator(): DirectCreator {
        val supers: MutableList<TypeHolder.Static> = ArrayList(nonGenericSuperCount())
        val sam = samType?.let { samInfo ->
            SamType.DirectSam(
                    explicit = samInfo.explicit,
                    inputs = samInfo.signature.inputs.map { it.second.convert().holder() },
                    output = samInfo.signature.output.convert().holder()
            )
        }
        val unreadyType = DirectType(
                minInfo = info.toMinimalInfo(internCache),
                virtual = false,
                superTypes = supers,
                samType = sam
        )

        return DirectCreator(
                unfinishedType = unreadyType,
                directSupers = directSupers.infoMutableConverted(),
                satSupers = satSupers.cmiMutableConverted(),
                nonGenericAppend = supers::add
        )
    }

    fun SemiInfo.TemplateInfo.initCreator(): TemplateCreator {
        val supers: MutableList<TypeHolder<*, *>> = ArrayList(nonGenericSuperCount() + datSupers.size)

        fun TypeParamInfo.toAp(): ApplicationParameter? {
            return when (this) {
                TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
                TypeParamInfo.SelfRef -> SelfSubstitution
                is TypeParamInfo.BoundedWildcard -> {
                    val bound = bound.toAp() as? Substitution ?: return null
                    val dir = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) UPPER else LOWER
                    BoundedWildcard.Dynamic(param = bound, direction = dir)
                }
                is TypeParamInfo.Direct -> StaticTypeSubstitution(arg.toMinimalInfo(internCache).complete().holder())
                is TypeParamInfo.Sat -> StaticTypeSubstitution(sat.convert().holder())
                is TypeParamInfo.Dat -> {
                    val convertedArgs = dat.args.map { it.toAp() }.liftNull() ?: return null
                    val convertedDat = TypeHolder.Dynamic.Indirect(
                            CompleteMinInfo.Dynamic(dat.template.toMinimalInfo(internCache), convertedArgs)
                    )
                    TypeSubstitution(convertedDat)
                }
                is TypeParamInfo.Param -> ParamSubstitution(param)
            }
        }

        val sam = samType?.let { samInfo ->
            SamType.GenericSam(
                    explicit = samInfo.explicit,
                    inputs = samInfo.signature.inputs.map { it.second.toAp() }.liftNull() ?: return@let null,
                    output = samInfo.signature.output.toAp() ?: return@let null
            )
        }

        val convertedTps = typeParams.map { tp ->
            val bounds: List<Substitution> = tp.bounds.map { it.toAp() as? Substitution }.liftNull() ?: emptyList()
            TypeParameter(
                    sign = tp.sign,
                    bounds = TypeBounds(bounds.toSet())
            )
        }

        val unreadyType = TypeTemplate(
                info = info.toMinimalInfo(internCache),
                typeParams = convertedTps,
                superTypes = supers,
                samType = sam,
                virtual = false
        )

        return TemplateCreator(
                unfinishedType = unreadyType,
                mutableSupers = supers,
                directSupers = directSupers.infoMutableConverted(),
                satSupers = satSupers.cmiMutableConverted(),
                datSupers = datSupers.toMutableList()
        )
    }

    fun <I : SemiInfo<*>, C : SemiCreator<*>> creators(
            infos: Collection<I>,
            creatorInit: (I) -> C
    ): MutableMap<MinimalInfo, C> {
        return infos.groupBy(
                keySelector = { it.info.toMinimalInfo(internCache) },
                valueTransform = creatorInit
        ).mapValues { (_, v) -> v.first() }.toMutableMap()
    }

    fun directCreators(infos: TypeInfoResult): MutableMap<MinimalInfo, DirectCreator> {
        return creators(infos.directInfos) { it.initCreator() }
    }

    fun templateCreators(infos: TypeInfoResult): MutableMap<MinimalInfo, TemplateCreator> {
        return creators(infos.templateInfos) { it.initCreator() }
    }

    fun connectArtifact(): TypeResolver {
        connect()

        val repo = MapTypeRepo(
                directs = readyDirects,
                templates = readyTemplates
        )
        return SingleRepoTypeResolver(repo)
    }

    fun connectJcl(): JclTypeResult {
        readyTemplates[infoRepo.arrayType] = TypeTemplate(
                info = infoRepo.arrayType,
                superTypes = listOf(infoRepo.objectType.complete().holder()),
                typeParams = listOf(TypeParameter("X", TypeBounds(emptySet()))),
                samType = null
        )

        PrimitiveType.values().map(infoRepo::primitive).forEach { primitive ->
            readyDirects[primitive] = DirectType(minInfo = primitive, superTypes = emptyList(), samType = null, virtual = false)
        }

        connect()

        val jclTypeRepo = MapTypeRepo(
                directs = readyDirects,
                templates = readyTemplates
        )

        val javaRepo = DefaultJavaRepo.fromNullable(infoRepo, readyDirects::get)

        return JclTypeResult(javaRepo, jclTypeRepo)
    }

    init {
        allCreators.forEach { creator ->
            creator.directSupers.forEach { directSuper ->
                val unfinishedSuper = directCreators[directSuper]?.unfinishedType
                val superHolder = unfinishedSuper?.holder() ?: directSuper.complete().holder()
                creator.nonGenericAppend(superHolder)
            }
            creator.directSupers.clear()
        }
    }

    private fun <S : SemiType> findSemi(
            info: MinimalInfo,
            readyMap: Map<MinimalInfo, S>,
            creatorMap: Map<MinimalInfo, SemiCreator<S>>
    ): SemiResult<S> {
        readyMap[info]?.let { return SemiResult.Ready(it) }
        creatorMap[info]?.let { return SemiResult.Unready(it) }
        return SemiResult.NotFound()
    }

    private fun direct(info: MinimalInfo): SemiResult<DirectType> = findSemi(info, readyDirects, directCreators)

    private fun template(info: MinimalInfo): SemiResult<TypeTemplate> = findSemi(info, readyTemplates, templateCreators)

    private fun CompleteMinInfo.Static.convertAllowIndirect(): TypeHolder.Static {
        return if (args.isEmpty()) {
            when (val simpleDirect = direct(base)) {
                is SemiResult.Ready -> simpleDirect.readySemi.holder()
                is SemiResult.NotFound -> base.complete().holder()
                is SemiResult.Unready -> holder()
            }
        } else {
            when (val baseTemplate = template(base)) {
                is SemiResult.Ready -> {
                    val convertedArgs: List<TypeHolder.Static> = args.map { arg ->
                        arg.convertAllowIndirect()
                    }

                    baseTemplate.readySemi.staticApply(convertedArgs)?.holder()
                }
                else -> null
            } ?: holder()
        }
    }

    private fun CompleteMinInfo.Static.convert(rootIno: MinimalInfo): TypeHolder.Static? {
        if (base == rootIno) {
            return convertAllowIndirect()
        }

        return if (args.isEmpty()) {
            when (val simpleDirect = direct(base)) {
                is SemiResult.Ready -> simpleDirect.readySemi.holder()
                is SemiResult.NotFound -> base.complete().holder()
                is SemiResult.Unready -> null
            }
        } else {
            when (val baseTemplate = template(base)) {
                is SemiResult.Ready -> {
                    val convertedArgs: List<TypeHolder.Static> = args.map { arg ->
                        arg.convert(rootIno)
                    }.liftNull() ?: return null

                    baseTemplate.readySemi.staticApply(convertedArgs)?.holder()
                }
                is SemiResult.NotFound -> convertAllowIndirect()
                is SemiResult.Unready -> null
            }
        }
    }

    private fun TypeParamInfo.convertAllowIndirect(): ApplicationParameter {
        return when (this) {
            is TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
            is TypeParamInfo.SelfRef -> SelfSubstitution
            is TypeParamInfo.BoundedWildcard -> {
                val convertedBound = bound.convertAllowIndirect() as? Substitution ?: return TypeSubstitution.unboundedWildcard
                BoundedWildcard.Dynamic(
                        direction = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) UPPER else LOWER,
                        param = convertedBound
                )
            }
            is TypeParamInfo.Direct -> {
                when (val readyDirect = direct(arg.toMinimalInfo(internCache))) {
                    is SemiResult.Ready -> StaticTypeSubstitution(readyDirect.readySemi.holder())
                    else -> StaticTypeSubstitution(arg.toMinimalInfo(internCache).complete().holder())
                }
            }
            is TypeParamInfo.Sat -> StaticTypeSubstitution(sat.convert().convertAllowIndirect())
            is TypeParamInfo.Dat -> TypeSubstitution(dat.convertAllowIndirect())
            is TypeParamInfo.Param -> ParamSubstitution(param)
        }
    }

    private fun TypeParamInfo.convert(rootInfo: MinimalInfo): ApplicationParameter? {
        return when (this) {
            TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
            TypeParamInfo.SelfRef -> SelfSubstitution
            is TypeParamInfo.BoundedWildcard -> {
                val convertedBoundRaw = bound.convert(rootInfo) ?: return TypeSubstitution.unboundedWildcard
                val convertedBoundSub = convertedBoundRaw as? Substitution ?: StaticTypeSubstitution.unboundedWildcard
                BoundedWildcard.Dynamic(
                        direction = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) UPPER else LOWER,
                        param = convertedBoundSub
                )
            }
            is TypeParamInfo.Direct -> {
                if (arg.toMinimalInfo(internCache) == rootInfo) {
                    return StaticTypeSubstitution(arg.toMinimalInfo(internCache).complete().holder())
                }

                when (val readyDirect = direct(arg.toMinimalInfo(internCache))) {
                    is SemiResult.Ready -> StaticTypeSubstitution(readyDirect.readySemi.holder())
                    is SemiResult.NotFound -> StaticTypeSubstitution(arg.toMinimalInfo(internCache).complete().holder())
                    is SemiResult.Unready -> null
                }
            }
            is TypeParamInfo.Sat -> {
                val convertedSat = sat.convert().convert(rootInfo) ?: return null
                StaticTypeSubstitution(convertedSat)
            }
            is TypeParamInfo.Dat -> {
                val convertedDat = dat.convert(rootInfo) ?: return null
                TypeSubstitution(convertedDat)
            }
            is TypeParamInfo.Param -> ParamSubstitution(param)
        }
    }

    private fun DatInfo.convertAllowIndirect(): TypeHolder.Dynamic {
        val convertedArgs: List<ApplicationParameter> = args.map { arg ->
            arg.convertAllowIndirect()
        }

        return when (val baseTemplate = template(template.toMinimalInfo(internCache))) {
            is SemiResult.Ready -> {
                baseTemplate.readySemi.dynamicApply(convertedArgs)?.holder()
            }
            else -> null
        }.orElse {
            CompleteMinInfo.Dynamic(template.toMinimalInfo(internCache), convertedArgs).holder()
        }
    }

    private fun DatInfo.convert(rootInfo: MinimalInfo): TypeHolder.Dynamic? {
        if (template.toMinimalInfo(internCache) == rootInfo) {
            return convertAllowIndirect()
        }

        return when (val baseTemplate = template(template.toMinimalInfo(internCache))) {
            is SemiResult.Ready -> {
                val convertedArgs: List<ApplicationParameter> = args.map { arg ->
                    arg.convert(rootInfo)
                }.liftNull() ?: return null

                baseTemplate.readySemi.dynamicApply(convertedArgs)?.holder()
            }
            is SemiResult.NotFound -> convertAllowIndirect()
            is SemiResult.Unready -> null
        }
    }

    private fun connect() {
        fun <S: SemiType, C : SemiCreator<S>> moveDone(
                creators: MutableMap<MinimalInfo, C>,
                doneMap: MutableMap<MinimalInfo, S>
        ): Boolean {
            val removed = creators.mapNotNull { (info, creator) ->
                if (creator.done) {
                    doneMap[info] = creator.unfinishedType
                    info
                } else {
                    null
                }
            }

            removed.forEach { toRemove ->
                creators.remove(toRemove)
            }

            return removed.isNotEmpty()
        }

        while (allCreators.isNotEmpty()) {
            var iterationStat: HasUpdated = HasUpdated.None

            allCreators.forEach { creator ->
                iterationStat += creator.satSupers.removeIf { satSuper ->
                    satSuper.convert(creator.unfinishedType.info)?.also {
                        creator.nonGenericAppend(it)
                    } != null
                }
            }

            templateCreators.values.forEach { templateCreator ->
                iterationStat += templateCreator.datSupers.removeIf { datSuper ->
                    datSuper.convert(templateCreator.unfinishedType.info)?.also {
                        templateCreator.datSuperAppend(it)
                    } != null
                }
            }

            iterationStat += moveDone(directCreators, readyDirects)
            iterationStat += moveDone(templateCreators, readyTemplates)

            if (!iterationStat.hadUpdate) {
                println("Still present after direct connecting ==========>")
                allCreators.forEach { println(it.unfinishedType.info) }
                break
            }
        }

        allCreators.forEach { creator ->
            creator.satSupers.forEach { satSuper ->
                creator.nonGenericAppend(satSuper.convertAllowIndirect())
            }
        }

        templateCreators.values.forEach { templateCreator ->
            templateCreator.datSupers.forEach { datSuper ->
                templateCreator.datSuperAppend(datSuper.convertAllowIndirect())
            }
        }
    }

}