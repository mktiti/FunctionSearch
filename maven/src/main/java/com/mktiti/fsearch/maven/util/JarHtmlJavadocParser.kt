package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.javadoc.FunDocMap
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.util.zipIfSameLength
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.map
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipFile

// TODO - very much experimental
class JarHtmlJavadocParser(
        private val infoRepo: JavaInfoRepo
) {

    companion object {
        private val nameBlacklist = listOf(
                "package-use",
                "package-summary",
                "package-tree",
                "package-frame",
                "compact1-package-frame",
                "compact1-package-summary",
                "compact2-package-frame",
                "compact2-package-summary",
                "compact3-package-frame",
                "compact3-package-summary",
        )

        private fun Element.firstAttr(vararg names: String): String? {
            return names.map { attr(it) }.firstOrNull { it.isNotBlank() }
        }

        private val nonPrintRegex = "\\p{C}+".toRegex()

        private fun String.cleanText() = replace(nonPrintRegex, "").trim()

    }

    private fun parseParenFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        val (name, ins) = signature.split('(')
        val params = ins.dropLast(1).split(',').map { FunHeaderParser.parseParam(infoRepo, it) }

        return name to params
    }

    private fun parseDottedFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        val (name, ins) = signature.split('-', limit = 2)
        val params: List<FunIdParam> = if (ins == "-") {
            emptyList()
        } else {
            ins.split('-').filter { it.isNotEmpty() }.map { FunHeaderParser.parseParam(infoRepo, it) }
        }

        return name to params
    }

    private fun parseFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        return if ('(' in signature) {
            parseParenFunSignature(signature)
        } else {
            parseDottedFunSignature(signature)
        }
    }

    private data class IncompleteFunId(
            val name: String,
            val params: List<FunIdParam>,
            val relation: FunInstanceRelation
    ) {

        override fun equals(other: Any?): Boolean {
            return if (other is IncompleteFunId) {
                if (relation != other.relation || (name != other.name)) {
                    false
                } else {
                    params.zipIfSameLength(other.params)?.all { (a, b) ->
                        FunIdParam.equals(a, b, allowSimpleName = true)
                    } ?: false
                }
            } else {
                false
            }
        }

        override fun hashCode() = Objects.hash(name, relation)

    }

    private fun parseConstructorHeaders(file: MinimalInfo, document: Document): Map<IncompleteFunId, FunctionDoc> {
        val methodSummary = (
                document.selectFirst("[name='constructor_summary']") ?:
                document.selectFirst("[name='constructor.summary']") ?:
                document.getElementById("constructor.summary")
        )?.parent()?.selectFirst("table>tbody")

        if (methodSummary == null) {
            println("No constructor summary table found ($file)")
            return emptyMap()
        }

        return methodSummary.children().drop(1).mapNotNull {
            val (sigElem, descElem) = when (it.childrenSize()) {
                1 -> {
                    val sigDesc = it.child(0).children()
                    sigDesc[0] to sigDesc.getOrNull(1)
                }
                2 -> it.child(0) to it.child(1)
                else -> return@mapNotNull null
            }

            val (_, paramNames, signature) = FunHeaderParser.parseFunHeader(infoRepo, sigElem.wholeText()) ?: return@mapNotNull null
            val shortInfo = descElem?.wholeText()?.cleanText()

            IncompleteFunId(
                    name = "<init>",
                    relation = FunInstanceRelation.CONSTRUCTOR,
                    params = signature
            ) to FunctionDoc(
                    paramNames = paramNames,
                    shortInfo = shortInfo
            )
        }.toMap()
    }

    private fun parseMethodHeaders(file: MinimalInfo, document: Document): Map<IncompleteFunId, FunctionDoc> {
        val methodSummary = (
                document.selectFirst("[name='method_summary']") ?:
                document.selectFirst("[name='method.summary']") ?:
                document.getElementById("method.summary")
        )?.parent()?.selectFirst("table>tbody")

        if (methodSummary == null) {
            println("No method summary table found ($file)")
            return emptyMap()
        }

        return methodSummary.children().drop(1).mapNotNull {
            val (sigElem, descElem) = when (it.childrenSize()) {
                2 -> {
                    val sigDesc = it.child(1).children()
                    sigDesc[0] to sigDesc.getOrNull(1)
                }
                3 -> it.child(1) to it.child(2)
                else -> return@mapNotNull null
            }

            val (name, paramNames, signature) = FunHeaderParser.parseFunHeader(infoRepo, sigElem.wholeText()) ?: return@mapNotNull null
            val cleanName = name.cleanText()
            val isStatic = it.child(0).wholeText().cleanText().contains("static")

            val shortInfo = descElem?.wholeText()?.cleanText()

            IncompleteFunId(
                    name = cleanName,
                    relation = isStatic.map(FunInstanceRelation.STATIC, FunInstanceRelation.INSTANCE),
                    params = signature
            ) to FunctionDoc(
                    paramNames = paramNames,
                    shortInfo = shortInfo
            )
        }.toMap()
    }

    private data class DetailData(
            val signature: List<FunIdParam>,
            val detail: String
    )

    private fun parseConstructorDetails(file: MinimalInfo, document: Document): Map<IncompleteFunId, DetailData> {
        val constructorDetails = (
                document.selectFirst("[name='constructor_detail']") ?:
                document.selectFirst("[name='constructor.detail']") ?:
                document.getElementById("constructor.detail")
        )?.parent()

        if (constructorDetails == null) {
            println("No constructor detail elem found ($file)")
            return emptyMap()
        }

        return parseMemberDetails(constructorDetails, true)
    }

    private fun parseMethodDetails(file: MinimalInfo, document: Document): Map<IncompleteFunId, DetailData> {
        val methodDetails = (
                document.selectFirst("[name='method_detail']") ?:
                document.selectFirst("[name='method.detail']") ?:
                document.getElementById("method.detail")
        )?.parent()

        if (methodDetails == null) {
            println("No method detail elem found ($file)")
            return emptyMap()
        }

        return parseMemberDetails(methodDetails, false)
    }

    private fun parseMemberDetails(container: Element, isConstructor: Boolean): Map<IncompleteFunId, DetailData> {
        data class MethodGroup(
                val ids: List<Element>,
                val detailElem: Element
        )

        data class GroupAcc(
                val current: List<Element> = emptyList(),
                val finished: List<MethodGroup> = emptyList()
        ) {
            fun addId(idElem: Element) = copy(current = current + idElem)

            fun end(detailElem: Element) = GroupAcc(
                    finished = finished + MethodGroup(current, detailElem)
            )
        }

        val methodGroups = container.children().drop(2).fold(GroupAcc()) { acc, elem ->
            if (elem.tagName() == "a") {
                acc.addId(elem)
            } else {
                acc.end(elem)
            }
        }.finished

        return methodGroups.mapNotNull { (titleElems, detailElem) ->
            val titleElem = titleElems.lastOrNull() ?: return@mapNotNull null

            try {
                val funId = titleElem.firstAttr("id", "name") ?: return@mapNotNull null
                val (name, params) = parseFunSignature(funId)

                val detail = detailElem.child(0)

                val cleanName = name.cleanText()
                val relation = when {
                    isConstructor -> FunInstanceRelation.CONSTRUCTOR
                    detail.child(1).wholeText().cleanText().contains("static") -> FunInstanceRelation.STATIC
                    else -> FunInstanceRelation.INSTANCE
                }

                val info = IncompleteFunId(
                        name = if (relation == FunInstanceRelation.CONSTRUCTOR) "<init>" else cleanName,
                        relation = relation,
                        params = params
                )

                val fullDetails = detail.children().drop(2).joinToString("\n") {
                    it.wholeText().trim()
                }

                info to DetailData(params, fullDetails)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.toMap()
    }

    private fun parseFile(file: MinimalInfo, input: InputStream): List<Pair<FunctionInfo, FunctionDoc>> {
        val document = input.bufferedReader().use {
            Jsoup.parse(it.readText())
        }

        val methodInfos = parseConstructorHeaders(file, document) + parseMethodHeaders(file, document)
        val details = parseConstructorDetails(file, document) + parseMethodDetails(file, document)

        return methodInfos.mapNotNull { (id, doc) ->
            val data = details[id] ?: return@mapNotNull null

            val info = FunctionInfo(
                    file = file,
                    relation = id.relation,
                    name = id.name,
                    paramTypes = data.signature
            )

            val longInfo = doc.shortInfo?.let { data.detail.removePrefix(it).trimStart() } ?: data.detail

            info to doc.copy(longInfo = longInfo)
        }
    }

    fun parseJar(jarFile: File): FunDocMap? {
        return ZipFile(jarFile).use { jar ->
            (jar.getEntry("package-list") ?: jar.getEntry("element-list"))?.let { lister ->
                jar.getInputStream(lister).bufferedReader().useLines { lines ->
                    val packages = lines.filterNot {
                        it.isEmpty()
                    }.toList()

                    val storeMap = jar.entries().toList().map {
                        val (packageName, name) = it.name.split('/').cutLast()
                        Triple(packageName, name, it)
                    }.filter { (packageName, name, _) ->
                        val packageTest = packageName.joinToString(".").replace("/", ".")

                        (packageTest in packages) &&
                                name.endsWith(".html") &&
                                name.removeSuffix(".html") !in nameBlacklist
                    }.map { (packageName, name, entry) ->
                        try {
                            val info = MinimalInfo(
                                    packageName = packageName,
                                    simpleName = name.removeSuffix(".html")
                            )

                            parseFile(info, jar.getInputStream(entry))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emptyList()
                        }
                    }.flatten().toMap()

                    FunDocMap(storeMap)
                }
            }
        }
    }

}