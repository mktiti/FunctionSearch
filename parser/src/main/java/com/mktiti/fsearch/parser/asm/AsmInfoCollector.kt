package com.mktiti.fsearch.parser.asm

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.parser.intermediate.DefaultTypeParser
import com.mktiti.fsearch.parser.intermediate.JavaSignatureTypeParser
import com.mktiti.fsearch.parser.service.IndirectInfoCollector
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.mapMutablePrefixTree
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes

object AsmInfoCollector {

    fun collect(infoRepo: JavaInfoRepo, load: AsmCollectorView.() -> Unit): IndirectInfoCollector.IndirectInitialData {
        val visitor = AsmInfoCollectorVisitor(infoRepo)
        DefaultAsmCollectorView(visitor).load()
        return IndirectInfoCollector.IndirectInitialData(visitor.directTypes, visitor.typeTemplates)
    }

}
/*
private class ImTransformer(
        private val infoRepo: JavaInfoRepo,
        private val typeParams: List<String>
) {

    private fun transformStaticTypeBase(info: MinimalInfo, args: List<ApplicationParameter>): StaticTypeSubstitution? {
        val completeInfo: CompleteMinInfo.Static = if (args.isEmpty()) {
            info.complete()
        } else {
            val staticArgInfos = args.map {
                when (it) {
                    is TypeSubstitution<*, *> -> {
                        when (val holder = it.holder) {
                            is TypeHolder.Static -> holder.info
                            is TypeHolder.Dynamic -> null
                        }
                    }
                    else -> null
                }
            }.liftNull() ?: return null

            info.staticComplete(staticArgInfos)
        }
        return TypeSubstitution(completeInfo.holder())
    }

    private fun transformTypeBase(info: MinimalInfo, args: List<ImParam>): TypeSubstitution<*, *>? {
        val mappedArgs = args.map(this::transformDynamicArg).liftNull() ?: return null
        return transformStaticTypeBase(info, mappedArgs) ?: TypeSubstitution(info.dynamicComplete(mappedArgs).holder())
    }

    fun transformType(imType: ImParam.Type): TypeSubstitution<*, *>? = transformTypeBase(imType.info, imType.typeArgs)

    fun transformStaticType(imType: ImParam.Type): StaticTypeSubstitution? {
        val mappedArgs = imType.typeArgs.map(this::transformDynamicArg).liftNull() ?: return null
        return transformStaticTypeBase(imType.info, mappedArgs)
    }

    private fun transformArray(array: ImParam.Array): TypeSubstitution<*, *>? {
        return transformTypeBase(infoRepo.arrayType, listOf(array.type))
    }

    private fun transformWildcard(wildcardParam: ImParam, direction: BoundDirection): BoundedWildcard? {
        return when (val param = transformDynamicArg(wildcardParam)) {
            is TypeSubstitution<*, *> -> {
                when (val holder = param.holder) {
                    is TypeHolder.Static -> BoundedWildcard.Static(TypeSubstitution(holder), direction)
                    else -> BoundedWildcard.Dynamic(param, direction)
                }
            }
            is Substitution -> BoundedWildcard.Dynamic(param, direction)
            else -> null
        }
    }

    private fun transformDynamicArg(imParam: ImParam): ApplicationParameter? {
        fun infoType(getter: JavaInfoRepo.() -> MinimalInfo) = TypeSubstitution(infoRepo.getter().complete().holder())

        return when (imParam) {
            ImParam.Wildcard -> TypeSubstitution.unboundedWildcard
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

 */

private class AsmInfoCollectorVisitor(
        private val infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    private val typeParser: JavaSignatureTypeParser = DefaultTypeParser(infoRepo)

    val directTypes: MutablePrefixTree<String, DirectType> = mapMutablePrefixTree()
    val typeTemplates: MutablePrefixTree<String, TypeTemplate> = mapMutablePrefixTree()

    // val directInfos: MutablePrefixTree<String, CreatorInfo.Direct> = mapMutablePrefixTree()
    // val templateInfos: MutablePrefixTree<String, CreatorInfo.Template> = mapMutablePrefixTree()

    private data class TypeMeta(
            val info: MinimalInfo,
            val signature: String?,
            val superNames: List<String>,
            val nestDepth: Int? = null
    )

    private var currentType: TypeMeta? = null

    /*
    private fun addDirect(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo.Static>
    ) {
        directInfos.mutableSubtreeSafe(info.packageName)[info.simpleName] = CreatorInfo.Direct(
                info = info,
                directSupers = superTypes
        )
    }

    private fun addTemplate(
            info: MinimalInfo,
            typeParams: List<TypeParamInfo>,
            directSupers: List<CompleteMinInfo.Static>,
            datSupers: List<CompleteMinInfo.Dynamic>
    ) {
        templateInfos.mutableSubtreeSafe(info.packageName)[info.simpleName] = CreatorInfo.Template(
                info = info,
                typeParams = typeParams,
                directSupers = directSupers,
                datSupers = datSupers
        )
    }
    */

    private fun addDirect(type: DirectType) {
        directTypes.mutableSubtreeSafe(type.info.packageName)[type.info.simpleName] = type
    }

    private fun addDirect(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo.Static>
    ) {
        addDirect(
            DirectType(
                minInfo = info,
                superTypes = TypeHolder.staticIndirects(superTypes),
                samType = null,
                virtual = false
            )
        )
    }

    private fun addTemplate(template: TypeTemplate) {
        typeTemplates.mutableSubtreeSafe(template.info.packageName)[template.info.simpleName] = template
    }

    private fun addTemplate(
            info: MinimalInfo,
            superTypes: List<CompleteMinInfo<*>>,
            typeParams: List<TypeParameter>
    ) {
        addTemplate(
            TypeTemplate(
                info = info,
                superTypes = TypeHolder.anyIndirects(superTypes),
                typeParams = typeParams,
                samType = null,
                virtual = false
            )
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

        val nestContext: List<TypeParameter> = if (meta.nestDepth != null) {
            val nests = info.simpleName.split(".").dropLast(1)
            val depth = Integer.min(meta.nestDepth, nests.size)

            //val nestPackage: PrefixTree<String, CreatorInfo.Template> = templateInfos.mutableSubtreeSafe(info.packageName)
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

            if (signature == null) {
                addTemplate(
                        info = info,
                        typeParams = nestContext,
                        superTypes = superNames.map { directSuper ->
                            AsmUtil.parseName(directSuper).complete()
                        }
                )
            } else {
                when (val direct = typeParser.parseDirectTypeSignature(info, signature)) {
                    null -> {
                        addTemplate(typeParser.parseTemplateSignature(info, signature, nestContext))
                    }
                    else -> {
                        addDirect(direct)
                    }
                }
            }

            /*
            val type = if (signature == null) {
                ParsedType.Template(
                        typeParams = nestContext,
                        superTypes = superNames.map { ImParam.Type(AsmUtil.parseName(it), emptyList()) }
                )
            } else {
                TypeParser.parseType(signature)
            }

            val (ds, ts) = type.supersTypes.partition { it.typeArgs.isEmpty() }
            val directSupers: List<CompleteMinInfo.Static> = ds.map { it.info.complete() }

            when (type) {
                is ParsedType.Direct -> {
                    val transformer = ImTransformer(infoRepo, emptyList())
                    val templateSupers: List<CompleteMinInfo.Static> = ts.map { imSuper ->
                        transformer.transformStaticType(imSuper)?.holder?.info
                    }.liftNull() ?: error("Direct type '$info' has non-static supertypes (raw usage?)")

                    addDirect(
                            info = info,
                            superTypes = directSupers + templateSupers
                    )
                }
                is ParsedType.Template -> {
                    val paramNames = type.typeParams.map { it.sign }
                    val transformer = ImTransformer(infoRepo, paramNames)

                    val datSupers: List<CompleteMinInfo.Dynamic> = ts.map { imSuper ->
                        transformer.transformType(imSuper)?.holder?.info
                    }.liftNull()?.castIfAllInstance()!!

                    addTemplate(
                            info = info,
                            typeParams = type.typeParams,
                            directSupers = directSupers,
                            datSupers = datSupers
                    )
                }
            }
             */
        }
    }

}