package com.mktiti.fsearch.backend

import com.mktiti.fsearch.backend.api.relationDtoFromModel
import com.mktiti.fsearch.core.fit.*
import com.mktiti.fsearch.core.fit.FunInstanceRelation.*
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.util.genericString
import com.mktiti.fsearch.dto.FunDocDto
import com.mktiti.fsearch.dto.FunRelationDto
import com.mktiti.fsearch.dto.QueryFitResult
import com.mktiti.fsearch.util.PrefixTree
import com.mktiti.fsearch.util.mapMutablePrefixTree
import com.mktiti.fsearch.util.roll

interface FitPresenter {

    fun present(function: FunctionObj, fit: FittingMap, doc: FunctionDoc): QueryFitResult

    fun present(function: FunctionObj, doc: FunctionDoc): QueryFitResult

}

class BasicFitPresenter(
    private val infoRepo: JavaInfoRepo,
    private val silentPackages: PrefixTree<String, Boolean>
) : FitPresenter {

    companion object {
        private val defaultHidden = listOf(
                listOf("java", "lang") to true,
                listOf("java", "math") to true,
                listOf("java", "util") to false,
                listOf("java", "util", "function") to false,
                listOf("java", "time") to false
        )

        fun default(infoRepo: JavaInfoRepo) = from(infoRepo, defaultHidden)

        fun from(infoRepo: JavaInfoRepo, silentPackages: Collection<Pair<List<String>, Boolean>>): BasicFitPresenter {
            val tree = mapMutablePrefixTree<String, Boolean>()
            silentPackages.forEach { (packageParts, children) ->
                tree[packageParts] = children
            }
            return BasicFitPresenter(infoRepo, tree)
        }
    }

    private fun resolveInfo(info: MinimalInfo, currentPackage: PackageName): String {
        infoRepo.ifPrimitive(info)?.let {
            return it.javaName
        }

        val hide = info.packageName == currentPackage || info.packageName.roll(silentPackages to false) { acc, packagePart ->
            val (node, _) = acc
            when (val sub = node.subtree(listOf(packagePart))) {
                null -> (node to false) to true
                else -> (sub to true) to (sub.get() == true)
            }
        }.second

        return if (hide) info.simpleName else info.fullName
    }

    private fun <A : Any> resolveCompleteMinimal(currentPackage: PackageName, info: CompleteMinInfo<A>, argConvert: (A) -> String): String {
        return when {
            info.base.sameAs(infoRepo.arrayType) -> argConvert(info.args.single()) + "[]"
            info.base.virtual -> info.base.simpleName
            else -> {
                val base = resolveInfo(info.base, currentPackage)
                if (info.args.isEmpty()) {
                    base
                } else {
                    base + info.args.genericString { argConvert(it) }
                }
            }
        }
    }

    private fun resolveStaticName(currentPackage: PackageName, param: CompleteMinInfo.Static): String {
        return resolveCompleteMinimal(currentPackage, param) { arg ->
            resolveStaticName(currentPackage, arg)
        }
    }

    private fun resolveDynamicName(
            currentPackage: PackageName,
            param: CompleteMinInfo.Dynamic,
            tpNames: List<String>,
            selfName: String?
    ): String {
        return resolveCompleteMinimal(currentPackage, param) { arg ->
            resolveApplicationParam(currentPackage, arg, tpNames, selfName)
        }
    }

    private fun resolveApplicationParam(
            currentPackage: PackageName,
            param: ApplicationParameter,
            tpNames: List<String>,
            selfName: String?
    ): String {
        return when (param) {
            is Substitution -> resolveSub(currentPackage, param, tpNames, selfName)
            is BoundedWildcard -> {
                "? " + param.direction.keyword + " " + resolveSub(currentPackage, param.param, tpNames, selfName)
            }
        }
    }

    private fun resolveSub(currentPackage: PackageName, param: Substitution, tpNames: List<String>, selfName: String? = null): String {
        return when (param) {
            is Substitution.ParamSubstitution -> tpNames.getOrNull(param.param) ?: "#${param.param}"
            Substitution.SelfSubstitution -> selfName ?: "\$"
            is Substitution.TypeSubstitution<*, *> -> {
                when (val holder = param.holder) {
                    is TypeHolder.Static -> resolveStaticName(currentPackage, holder.info)
                    is TypeHolder.Dynamic -> resolveDynamicName(currentPackage, holder.info, tpNames, selfName)
                }
            }
        }
    }

    private fun Substitution.isDefaultTpBound(): Boolean {
        return (this as? Substitution.TypeSubstitution<*, *>)?.holder?.info?.base == infoRepo.objectType
    }

    private fun resolveTypeParam(currentPackage: PackageName, param: TypeParameter, names: List<String>) = buildString {
        append(param.sign)
        val nonTrivialBounds = param.bounds.upperBounds.filterNot { it.isDefaultTpBound() }
        if (nonTrivialBounds.isNotEmpty()) {
            nonTrivialBounds.joinToString(prefix = " extends ", separator = ", ", postfix = "") {
                resolveSub(currentPackage, it, names, param.sign)
            }
        }
    }

    private fun headerString(
            info: FunctionInfo,
            signature: TypeSignature,
            typeTps: List<String>,
            funTps: List<TypeParameter>,
            paramNames: List<String>?
    ): String {
        val currentPackage = info.file.packageName
        val tpNames = typeTps + funTps.map(TypeParameter::sign)

        return buildString {
            if (info.relation == CONSTRUCTOR) {
                append(info.file.simpleName)
            } else {
                if (info.relation == STATIC) {
                    append("static ")
                }
                if (funTps.isNotEmpty()) {
                    val tpString = funTps.genericString {
                        resolveTypeParam(currentPackage, it, tpNames)
                    }
                    append(tpString)
                    append(" ")
                }
                append(resolveSub(currentPackage, signature.output, tpNames))
                append(" ")
                append(info.name)
            }

            val explicitIns = if (info.relation == INSTANCE) {
                signature.inputParameters.drop(1)
            } else {
                signature.inputParameters
            }
            val inString = explicitIns.mapIndexed { i, inParam ->
                val (setName, param) = inParam
                val name = if (setName.startsWith("\$")) {
                    paramNames?.getOrNull(i) ?: setName
                } else {
                    setName
                }
                name to param
            }.joinToString(prefix = "(", separator = ", ", postfix = ")") {
                resolveSub(currentPackage, it.second, tpNames) + " " + it.first
            }
            append(inString)
        }
    }

    override fun present(function: FunctionObj, doc: FunctionDoc): QueryFitResult {
        val info = function.info
        val signature = function.signature

        val fileTypeParams = if (info.relation == STATIC) {
            0
        } else {
            val thisParam = if (info.relation == INSTANCE) {
                signature.inputParameters.firstOrNull()?.second
            } else {
                signature.output
            }
            (thisParam as? Substitution.TypeSubstitution<*, *>)?.typeParamCount ?: 0
        }

        val fileTps = signature.typeParameters.map(TypeParameter::sign).take(fileTypeParams)

        val fileName = info.file.fullName + (if (fileTps.isEmpty()) "" else fileTps.genericString())

        val header = headerString(
                info = info,
                signature = signature,
                typeTps = fileTps,
                funTps = signature.typeParameters.drop(fileTypeParams),
                paramNames = doc.paramNames
        )

        return QueryFitResult(
                file = fileName,
                funName = info.name,
                relation = relationDtoFromModel(info.relation),
                doc = FunDocDto(shortInfo = doc.shortInfo, details = doc.longInfo),
                header = header
        )
    }

    override fun present(function: FunctionObj, fit: FittingMap, doc: FunctionDoc): QueryFitResult {
        return present(function, doc)
    }

}
