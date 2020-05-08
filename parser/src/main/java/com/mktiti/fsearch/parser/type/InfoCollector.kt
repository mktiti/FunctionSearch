package com.mktiti.fsearch.parser.type

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.SuperType.DynamicSuper
import com.mktiti.fsearch.core.type.SuperType.StaticSuper
import com.mktiti.fsearch.core.type.SuperType.StaticSuper.EagerStatic
import com.mktiti.fsearch.parser.function.ImParam
import com.mktiti.fsearch.util.MutablePrefixTree
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.mapMutablePrefixTree
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.util.*
import kotlin.collections.ArrayList

class InfoCollector(
        private val artifact: String,
        private val infoRepo: JavaInfoRepo
) : ClassVisitor(Opcodes.ASM8) {

    companion object {
        fun parseNonGeneric(type: String): MinimalInfo {
            val splitName = type.split('$', '/')
            val (packageName, simpleName) = splitName.cutLast()
            return MinimalInfo(packageName, simpleName)
        }
    }

    val directTypes: MutablePrefixTree<String, DirectCreator> = mapMutablePrefixTree()
    val templateTypes: MutablePrefixTree<String, TemplateCreator> = mapMutablePrefixTree()

    private fun addUnfinishedDirect(
            info: TypeInfo,
            superCount: Int,
            directSupers: List<MinimalInfo>,
            templateSupers: MutableList<DatCreator>
    ) {
        val supers: MutableList<StaticSuper> = ArrayList(superCount)
        val created = Type.NonGenericType.DirectType(info, supers)

        directTypes.mutableSubtreeSafe(info.packageName)[info.name] = DirectCreator(
                unfinishedType = created,
                addNonGenericSuper = { superType -> supers += EagerStatic(superType) },
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

        templateTypes.mutableSubtreeSafe(info.packageName)[info.name] = TemplateCreator(
                unfinishedType = created,
                directSuperAppender = { superType -> supers += EagerStatic(superType) },
                templateSuperAppender = { superType -> supers += DynamicSuper.EagerDynamic(superType) },
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
            is ImParam.TypeParamRef -> TypeArgCreator.Param(typeParams.indexOf(imParam.sign))
            is ImParam.Array -> TypeArgCreator.Dat(DatCreator(
                    template = infoRepo.arrayType,
                    args = listOf(transformArg(imParam.type, typeParams))
            ))
            is ImParam.Primitive -> TypeArgCreator.Direct(infoRepo.primitive(imParam.value))
            is ImParam.UpperWildcard -> TypeArgCreator.UpperWildcard(transformArg(imParam.param, typeParams))
            is ImParam.LowerWildcard -> error("Lower wildcard (? super ...) should not be possible in type declaration")
            ImParam.Void -> TypeArgCreator.Direct(infoRepo.voidType)
        }
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        if (access and Opcodes.ACC_PUBLIC == 0) {
            return
        }

        val superCount = (if (superName == null) 0 else 1) + (interfaces?.size ?: 0)
        val superNames: List<String> = ArrayList<String>(superCount).apply {
            superName?.let(this::add)
            interfaces?.let(this::addAll)
        }

        val info = parseNonGeneric(name).full(artifact)

        if (signature == null) {
            addUnfinishedDirect(
                    info = info,
                    superCount = superCount,
                    directSupers = superNames.map { parseNonGeneric(it) }.toMutableList(),
                    templateSupers = LinkedList()
            )
        } else {
            val type = parseType(signature)
            val (ds, ts) = type.supersTypes.partition { it.typeArgs.isEmpty() }

            when (type) {
                is ParsedType.Direct -> {
                    val templateSupers = ts.map { imSuper ->
                        DatCreator(
                                template = imSuper.info,
                                args = imSuper.typeArgs.map { transformArg(it, emptyList()) }
                        )
                    }

                    addUnfinishedDirect(
                            info = info,
                            superCount = superCount,
                            directSupers = ds.map { it.info },
                            templateSupers = templateSupers.toMutableList()
                    )
                }
                is ParsedType.Template -> {
                    val params = type.typeParams.map { it.sign }

                    val templateSupers = ts.map { imSuper ->
                        DatCreator(
                                template = imSuper.info,
                                args = imSuper.typeArgs.map { transformArg(it, params) }
                        )
                    }

                    addUnfinishedTemplate(
                            info = info,
                            superCount = superCount,
                            typeParams = params,
                            directSupers = ds.map { it.info },
                            templateSupers = templateSupers.toMutableList()
                    )
                }
            }
        }
    }

}