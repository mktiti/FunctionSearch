package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.*
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard.BoundDirection.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.service.indirect.*
import com.mktiti.fsearch.parser.util.JavaTypeParseLog
import com.mktiti.fsearch.util.orElse

class JavaTypeInfoConnector(
        private val infoRepo: JavaInfoRepo,
        private val log: JavaTypeParseLog
) : TypeInfoConnector {

    private fun <T> useOneshot(
            rawInfoResults: RawTypeInfoResult,
            code: OneshotConnector.() -> T
    ) = OneshotConnector(infoRepo, rawInfoResults).code()

    override fun connectJcl(rawInfoResults: RawTypeInfoResult) = useOneshot(rawInfoResults) {
        connectJcl()
    }

    override fun connectArtifact(rawInfoResults: RawTypeInfoResult) = useOneshot(rawInfoResults) {
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
        rawInfoResults: RawTypeInfoResult
) {

    companion object {
        fun SemiInfo.DirectInfo.initCreator(): DirectCreator {
            val supers: MutableList<TypeHolder.Static> = ArrayList(nonGenericSuperCount)
            val sam = samType?.let { samInfo ->
                SamType.DirectSam(
                        explicit = samInfo.explicit,
                        inputs = samInfo.signature.inputs.map { it.second.holder() },
                        output = samInfo.signature.output.holder()
                )
            }
            val unreadyType = DirectType(
                    minInfo = info,
                    virtual = false,
                    superTypes = supers,
                    samType = sam
            )

            return DirectCreator(
                    unfinishedType = unreadyType,
                    directSupers = directSupers.toMutableList(),
                    satSupers = satSupers.toMutableList(),
                    nonGenericAppend = supers::add
            )
        }

        fun SemiInfo.TemplateInfo.initCreator(): TemplateCreator {
            val supers: MutableList<TypeHolder<*, *>> = ArrayList(nonGenericSuperCount + datSupers.size)

            fun TypeParamInfo.toAp(): ApplicationParameter? {
                return when (this) {
                    TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
                    TypeParamInfo.SelfRef -> SelfSubstitution
                    is TypeParamInfo.BoundedWildcard -> {
                        val bound = bound.toAp() as? Substitution ?: return null
                        val dir = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) UPPER else LOWER
                        BoundedWildcard.Dynamic(param = bound, direction = dir)
                    }
                    is TypeParamInfo.Direct -> StaticTypeSubstitution(arg.complete().holder())
                    is TypeParamInfo.Sat -> StaticTypeSubstitution(sat.holder())
                    is TypeParamInfo.Dat -> {
                        val convertedArgs = dat.args.map { it.toAp() }.liftNull() ?: return null
                        val convertedDat = TypeHolder.Dynamic.Indirect(
                                CompleteMinInfo.Dynamic(dat.template, convertedArgs)
                        )
                        TypeSubstitution(convertedDat)
                    }
                    is TypeParamInfo.Param -> Substitution.ParamSubstitution(param)
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
                    info = info,
                    typeParams = convertedTps,
                    superTypes = supers,
                    samType = sam,
                    virtual = false
            )

            return TemplateCreator(
                    unfinishedType = unreadyType,
                    mutableSupers = supers,
                    directSupers = directSupers.toMutableList(),
                    satSupers = satSupers.toMutableList(),
                    datSupers = datSupers.toMutableList()
            )
        }

        fun <I : SemiInfo<*>, C : SemiCreator<*>> creators(
                infos: Collection<I>,
                creatorInit: (I) -> C
        ): MutableMap<MinimalInfo, C> {
            return infos.groupBy(
                    keySelector = SemiInfo<*>::info,
                    valueTransform = creatorInit
            ).mapValues { (_, v) -> v.first() }.toMutableMap()
        }

        fun directCreators(infos: RawTypeInfoResult): MutableMap<MinimalInfo, DirectCreator> {
            return creators(infos.directInfos) { it.initCreator() }
        }

        fun templateCreators(infos: RawTypeInfoResult): MutableMap<MinimalInfo, TemplateCreator> {
            return creators(infos.templateInfos) { it.initCreator() }
        }
    }

    private val directCreators: MutableMap<MinimalInfo, DirectCreator> = directCreators(rawInfoResults)
    private val templateCreators: MutableMap<MinimalInfo, TemplateCreator> = templateCreators(rawInfoResults)
    private val allCreators: Collection<SemiCreator<*>>
        get() = listOf(directCreators, templateCreators).flatMap { it.values }

    private val readyDirects: MutableMap<MinimalInfo, DirectType> = HashMap()
    private val readyTemplates: MutableMap<MinimalInfo, TypeTemplate> = HashMap()

    fun connectArtifact(): TypeResolver {
        connect()

        val repo = MapTypeRepo(
                directs = readyDirects,
                templates = readyTemplates
        )
        return SingleRepoTypeResolver(repo)
    }

    fun connectJcl(): TypeInfoConnector.JclResult {
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

        return TypeInfoConnector.JclResult(javaRepo, jclTypeRepo)
    }

    init {
        allCreators.forEach { creator ->
            creator.directSupers.forEach { directSuper ->
                val unfinishedSuper = directCreators[directSuper]?.unfinishedType
                if (unfinishedSuper != null) {
                    creator.nonGenericAppend(unfinishedSuper.holder())
                } else {
                    creator.nonGenericAppend(directSuper.complete().holder())
                }
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
            TypeParamInfo.Wildcard -> TypeSubstitution.unboundedWildcard
            TypeParamInfo.SelfRef -> SelfSubstitution
            is TypeParamInfo.BoundedWildcard -> {
                val convertedBound = bound.convertAllowIndirect() as? Substitution ?: return TypeSubstitution.unboundedWildcard
                BoundedWildcard.Dynamic(
                        direction = if (this is TypeParamInfo.BoundedWildcard.UpperWildcard) UPPER else LOWER,
                        param = convertedBound
                )
            }
            is TypeParamInfo.Direct -> {
                when (val readyDirect = direct(arg)) {
                    is SemiResult.Ready -> StaticTypeSubstitution(readyDirect.readySemi.holder())
                    else -> StaticTypeSubstitution(arg.complete().holder())
                }
            }
            is TypeParamInfo.Sat -> StaticTypeSubstitution(sat.convertAllowIndirect())
            is TypeParamInfo.Dat -> TypeSubstitution(dat.convertAllowIndirect())
            is TypeParamInfo.Param -> Substitution.ParamSubstitution(param)
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
                if (arg == rootInfo) {
                    return StaticTypeSubstitution(arg.complete().holder())
                }

                when (val readyDirect = direct(arg)) {
                    is SemiResult.Ready -> StaticTypeSubstitution(readyDirect.readySemi.holder())
                    is SemiResult.NotFound -> StaticTypeSubstitution(arg.complete().holder())
                    is SemiResult.Unready -> null
                }
            }
            is TypeParamInfo.Sat -> {
                val convertedSat = sat.convert(rootInfo) ?: return null
                StaticTypeSubstitution(convertedSat)
            }
            is TypeParamInfo.Dat -> {
                val convertedDat = dat.convert(rootInfo) ?: return null
                TypeSubstitution(convertedDat)
            }
            is TypeParamInfo.Param -> Substitution.ParamSubstitution(param)
        }
    }

    private fun DatInfo.convertAllowIndirect(): TypeHolder.Dynamic {
        val convertedArgs: List<ApplicationParameter> = args.map { arg ->
            arg.convertAllowIndirect()
        }

        return when (val baseTemplate = template(template)) {
            is SemiResult.Ready -> {
                baseTemplate.readySemi.dynamicApply(convertedArgs)?.holder()
            }
            else -> null
        }.orElse {
            CompleteMinInfo.Dynamic(template, convertedArgs).holder()
        }
    }

    private fun DatInfo.convert(rootInfo: MinimalInfo): TypeHolder.Dynamic? {
        if (template == rootInfo) {
            return convertAllowIndirect()
        }

        return when (val baseTemplate = template(template)) {
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

    /*
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
                                    is DynamicAppliedType -> {
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
                                    is DynamicAppliedType -> creator.templateSuperAppender(superType)
                                }
                            }
                        }
                        true
                    }
                }
            }
        }
    }

     */

}