package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.core.javadoc.DocStore
import com.mktiti.fsearch.core.javadoc.FunctionDoc
import com.mktiti.fsearch.core.javadoc.SingleDocMapStore
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.orElse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.io.InputStream
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
                "package-frame"
        )

        private fun Element.firstAttr(vararg names: String): String? {
            return names.map { attr(it) }.firstOrNull { it.isNotBlank() }
        }
    }

    private fun parseParam(param: String): FunIdParam {
        if (param.endsWith("...") || param.endsWith("[]")) {
            // TODO
            return FunIdParam.Type(infoRepo.arrayType)
        }

        return PrimitiveType.fromNameSafe(param)?.let {
            FunIdParam.Type(infoRepo.primitive(it))
        }.orElse {
            val parsed: FunIdParam = if ('.' !in param && param.all { it.isDigit() || it.isUpperCase() }) {
                FunIdParam.TypeParam(param)
            } else {
                val (pack, type) = param.split('.').cutLast()
                FunIdParam.Type(MinimalInfo(pack, type))
            }

            parsed
        }
    }
    private fun parseParenedFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        val (name, ins) = signature.split('(')
        val params = ins.dropLast(1).split(',').map { parseParam(it) }

        return name to params
    }

    private fun parseDottedFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        val (name, ins) = signature.split('-', limit = 2)
        val params: List<FunIdParam> = if (ins == "-") {
            emptyList()
        } else {
            ins.split('-').filter { it.isNotEmpty() }.map { parseParam(it) }
        }

        return name to params
    }

    private fun parseFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        return if ('(' in signature) {
            parseParenedFunSignature(signature)
        } else {
            parseDottedFunSignature(signature)
        }
    }

    private fun parseFile(file: MinimalInfo, input: InputStream): List<Pair<FunctionInfo, FunctionDoc>> {
        val document = input.bufferedReader().use {
            Jsoup.parse(it.readText())
        }

        val methodDetails = (
                document.selectFirst("[name='method_detail']") ?:
                document.selectFirst("[name='method.detail']") ?:
                document.getElementById("method.detail")
        )?.parent()

        if (methodDetails == null) {
            println("No method detail elem found ($file)")
            return emptyList()
        }

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

        val methodGroups = methodDetails.children().drop(2).fold(GroupAcc()) { acc, elem ->
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
                val isStatic = detail.child(1).wholeText().split("\\W+".toRegex()).contains("static")

                val info = FunctionInfo(
                        file = file,
                        name = name,
                        isStatic = isStatic,
                        paramTypes = params
                )

                val fullDetails = detail.children().drop(2).joinToString("\n") { it.wholeText() }

                val doc = FunctionDoc(
                        link = null,
                        paramNames = null,
                        shortInfo = null,
                        longInfo = fullDetails
                )

                info to doc
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun parseJar(jarFile: File): DocStore {
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
                            emptyList<Pair<FunctionInfo, FunctionDoc>>()
                        }
                    }.flatten().toMap()

                    SingleDocMapStore(storeMap)
                }
            } ?: DocStore.nop()
        }
    }

}