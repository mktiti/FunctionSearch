package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection.LOWER
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard.Bounded.BoundDirection.UPPER
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.castIfAllInstance
import com.mktiti.fsearch.core.util.liftNull
import com.mktiti.fsearch.parser.function.ImParam
import com.mktiti.fsearch.parser.function.ImTypeParam
import com.mktiti.fsearch.parser.service.IndirectInfoCollector
import com.mktiti.fsearch.parser.util.AsmUtil
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.mapMutablePrefixTree
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.io.InputStream

interface AsmInfoCollectorView {

    fun loadEntry(input: InputStream)

}

object AsmInfoCollector {

    fun collect(artifact: String, infoRepo: JavaInfoRepo, load: AsmInfoCollectorView.() -> Unit): IndirectInfoCollector.IndirectInitialData {
        val visitor = AsmInfoCollectorVisitor(artifact, infoRepo)
        object : AsmInfoCollectorView {
            override fun loadEntry(input: InputStream) {
                ClassReader(input).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            }
        }.load()
        return IndirectInfoCollector.IndirectInitialData(visitor.directTypes, visitor.typeTemplates)
    }

}

private class ImTransformer(
        private val infoRepo: JavaInfoRepo,
        private val typeParams: List<String>
) {

    private fun transformTypeBase(info: MinimalInfo, args: List<ImParam>): Substitution.TypeSubstitution<*, *>? {
        return if (args.isEmpty()) {
            StaticTypeSubstitution(info.complete())
        } else {
            val mappedArgs = args.map(this::transformDynamicArg).liftNull() ?: return null
            val staticArgs = mappedArgs.castIfAllInstance<StaticTypeSubstitution>()

            if (staticArgs != null) {
                StaticTypeSubstitution(info.staticComplete(staticArgs.map { it.type }))
            } else {
                DynamicTypeSubstitution(info.dynamicComplete(mappedArgs))
            }
        }
    }

    fun transformType(imType: ImParam.Type): Substitution.TypeSubstitution<*, *>? {
        return transformTypeBase(imType.info, imType.typeArgs)
    }

    fun transformStaticType(imType: ImParam.Type): StaticTypeSubstitution? = transformType(imType) as? StaticTypeSubstitution

    private fun transformArray(array: ImParam.Array): Substitution.TypeSubstitution<*, *>? {
        return transformTypeBase(infoRepo.arrayType, listOf(array.type))
    }

    private fun transformWildcard(wildcardParam: ImParam, direction: BoundDirection): Wildcard.Bounded? {
        return when (val param = transformDynamicArg(wildcardParam)) {
            is Substitution -> Wildcard.Bounded(param, direction)
            else -> null
        }
    }

    private fun transformDynamicArg(imParam: ImParam): ApplicationParameter? {
        fun infoType(getter: JavaInfoRepo.() -> MinimalInfo) = StaticTypeSubstitution(infoRepo.getter().complete())

        return when (imParam) {
            ImParam.Wildcard -> Wildcard.Direct
            is ImParam.UpperWildcard -> transformWildcard(imParam.param, UPPER)
            is ImParam.LowerWildcard -> transformWildcard(imParam.param, LOWER)
            is ImParam.Primitive -> infoType { primitive(imParam.value) }
            is ImParam.Array -> transformArray(imParam)
            is ImParam.Type -> transformType(imParam)
            is ImParam.TypeParamRef -> ParamSubstitution(typeParams.withIndex().find { it.value == imParam.sign }?.index ?: error("Invalid type param sub."))
            ImParam.Void -> infoType { voidType }
        }
    }

}

private class AsmInfoCollectorVisitor(
        private val artifact: String,
        private val infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    val directTypes: MutablePrefixTree<String, DirectType> = mapMutablePrefixTree()
    val typeTemplates: MutablePrefixTree<String, TypeTemplate> = mapMutablePrefixTree()

    private data class TypeMeta(
            val info: MinimalInfo,
            val signature: String?,
            val superNames: List<String>,
            val nestDepth: Int? = null
    )

    private var currentType: TypeMeta? = null

    private fun addDirect(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo.Static>
    ) {
        directTypes.mutableSubtreeSafe(info.packageName)[info.simpleName] = DirectType(
                minInfo = info,
                superTypes = superTypes,
                samType = null,
                virtual = false
        )
    }

    private fun addTemplate(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo<*>>,
            typeParams: List<TypeParameter>
    ) {
        typeTemplates.mutableSubtreeSafe(info.packageName)[info.simpleName] = TypeTemplate(
                info = info,
                superTypes = superTypes,
                typeParams = typeParams,
                samType = null,
                virtual = false
        )
    }

    /*
    private fun addUnfinishedDirect(
            info: TypeInfo,
            superCount: Int,
            directSupers: List<MinimalInfo>,
            templateSupers: MutableList<DatCreator>
    ) {
        val supers: MutableList<SuperType.StaticSuper> = ArrayList(superCount)
        val created = DirectType(info, supers)

        loadedDirects.mutableSubtreeSafe(info.packageName)[info.name] = DirectCreator(
                unfinishedType = created,
                addNonGenericSuper = { superType -> supers += SuperType.StaticSuper.EagerStatic(superType) },
                templateSupers = templateSupers,
                directSupers = directSupers
        )
    }

    private fun addUnfinishedTemplate(
            info: TypeInfo,
            superCount: Int,
            typeParams: List<String>,
            directSupers: List<MinimalInfo>,
            templateSupers: MutableList<DatCreator>
    ) {
        val supers: MutableList<SuperType<Type>> = ArrayList(superCount)
        val created = TypeTemplate(info, typeParams.map { TypeParameter(it, TypeBounds(emptySet())) }, supers)

        loadedTemplates.mutableSubtreeSafe(info.packageName)[info.name] = TemplateCreator(
                unfinishedType = created,
                directSuperAppender = { superType -> supers += SuperType.StaticSuper.EagerStatic(superType) },
                templateSuperAppender = { superType -> supers += SuperType.DynamicSuper.EagerDynamic(superType) },
                templateSupers = templateSupers,
                directSupers = directSupers
        )
    }


    private fun transformArg(imParam: ImParam, typeParams: List<String>): TypeArgCreator {
        return when (imParam) {
            ImParam.Wildcard -> TypeArgCreator.Wildcard
            is ImParam.Type -> {
                if (imParam.typeArgs.isEmpty()) {
                    TypeArgCreator.Direct(imParam.info)
                } else {
                    TypeArgCreator.Dat(
                            DatCreator(
                                    template = imParam.info,
                                    args = imParam.typeArgs.map { transformArg(it, typeParams) }
                            )
                    )
                }
            }
            is ImParam.TypeParamRef -> TypeArgCreator.Param(
                    typeParams.withIndex().find { it.value == imParam.sign }?.index ?: error("ASD")
            )
            is ImParam.Array -> TypeArgCreator.Dat(DatCreator(
                    template = infoRepo.arrayType,
                    args = listOf(transformArg(imParam.type, typeParams))
            ))
            is ImParam.Primitive -> TypeArgCreator.Direct(infoRepo.primitive(imParam.value))
            is ImParam.UpperWildcard -> TypeArgCreator.UpperWildcard(transformArg(imParam.param, typeParams))
            is ImParam.LowerWildcard -> {
                println("Lower wildcard (? super ...) should not be possible in type declaration")
                TypeArgCreator.Wildcard
            }
            ImParam.Void -> TypeArgCreator.Direct(infoRepo.voidType)
        }
    }
     */

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        // if (access and Opcodes.ACC_PUBLIC == 0) {
        //    return
        // }

        val info = AsmUtil.parseName(name)

        val lastName = info.simpleName.split('.').last()
        if (lastName.first().isDigit()) {
            // Skip anonymous nested and local class
            return
        }

        val superCount = (if (superName == null) 0 else 1) + (interfaces?.size ?: 0)
        val superNames: List<String> = ArrayList<String>(superCount).apply {
            superName?.let(this::add)
            interfaces?.let(this::addAll)
        }

        currentType = TypeMeta(info, signature, superNames)
    }

    override fun visitField(access: Int, name: String, descriptor: String?, signature: String?, value: Any?): FieldVisitor? {
        if (name.startsWith("this$")) {
            val depth = name.removePrefix("this$").toIntOrNull()?.let { it + 1 } ?: return null
            currentType?.let {
                if (it.nestDepth == null || depth > it.nestDepth) {
                    currentType = it.copy(nestDepth = depth)
                }
            }
        }

        return null
    }

    override fun visitEnd() {
        currentType?.let {
            createInitials(it)
        }
        currentType = null
    }

    private fun createInitials(meta: TypeMeta) {
        val (info, signature, superNames) = meta
        val superCount = superNames.size

        val nestContext = if (meta.nestDepth != null) {
            val nests = info.simpleName.split(".").dropLast(1)
            val depth = Integer.min(meta.nestDepth, nests.size)

            val nestPackage: PrefixTree<String, TypeTemplate> = typeTemplates.mutableSubtreeSafe(info.packageName)

            val affectingNests = nests.takeLast(depth)
            val nestPrefix = when (val prefix = nests.dropLast(depth).joinToString(separator = ".")) {
                "" -> prefix
                else -> "$prefix."
            }

            affectingNests.fold(emptyList<TypeParameter>() to nestPrefix) { (params, name), nest ->
                val newName = name + nest
                val nestParams = nestPackage[newName]?.typeParams ?: emptyList()

                (params + nestParams) to "$newName."
            }.first
        } else {
            emptyList()
        }

        if (signature == null && nestContext.isEmpty()) {
            addDirect(
                    info = info,
                    superTypes = superNames.map(AsmUtil::parseCompleteStaticName)
            )
        } else {
            val nestTypeParams = nestContext.map {
                // TODO param bounds
                ImTypeParam(it.sign, emptyList())
            }

            val type = if (signature == null) {
                ParsedType.Template(
                        typeParams = nestTypeParams,
                        superTypes = superNames.map { ImParam.Type(AsmUtil.parseName(it), emptyList()) }
                )
            } else {
                parseType(signature, nestTypeParams)
            }
            val (ds, ts) = type.supersTypes.partition { it.typeArgs.isEmpty() }
            val directSupers: List<CompleteMinInfo.Static> = ds.map { it.info.complete() }

            when (type) {
                is ParsedType.Direct -> {
                    val transformer = ImTransformer(infoRepo, emptyList())
                    val templateSupers: List<CompleteMinInfo.Static> = ts.map { imSuper ->
                        transformer.transformStaticType(imSuper)?.type
                    }.liftNull() ?: error("Direct type '$info' has non-static supertypes (raw usage?)")

                    addDirect(
                            info = info,
                            superTypes = directSupers + templateSupers
                    )
                }
                is ParsedType.Template -> {
                    val params = type.typeParams.map { it.sign }
                    val transformer = ImTransformer(infoRepo, params)

                    val templateSupers: List<CompleteMinInfo<*>> = ts.map { imSuper ->
                        transformer.transformType(imSuper)?.type
                    }.liftNull()!!

                    addTemplate(
                            info = info,
                            typeParams = params.map { TypeParameter(it, upperBounds(StaticTypeSubstitution(infoRepo.objectType.complete()))) }, // TODO
                            superTypes = directSupers + templateSupers
                    )
                }
            }
        }
    }

}