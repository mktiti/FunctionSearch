package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.core.fit.FunIdParam
import com.mktiti.fsearch.core.fit.FunctionInfo
import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType
import com.mktiti.fsearch.modules.javadoc.DocStore
import com.mktiti.fsearch.modules.javadoc.FunctionDoc
import com.mktiti.fsearch.modules.javadoc.SingleDocMapStore
import com.mktiti.fsearch.util.cutLast
import com.mktiti.fsearch.util.orElse
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

// TODO - very much experimental
class JarHtmlJavadocParser(
        private val infoRepo: JavaInfoRepo
) {

    private fun parseFunSignature(signature: String): Pair<String, List<FunIdParam>> {
        val (name, ins) = signature.split('(')
        val params = ins.dropLast(1).split(',').map { param ->
            PrimitiveType.fromNameSafe(param)?.let {
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

        return name to params
    }

    private fun parseFile(file: MinimalInfo, input: InputStream): List<Pair<FunctionInfo, FunctionDoc>> {
        val document = input.bufferedReader().use {
            Jsoup.parse(it.readText())
        }

        val detailElem = (
                document.selectFirst("[name='method_detail']") ?: document.getElementById("method.detail")
        )?.parent()

        if (detailElem == null) {
            println("No method detail elem found")
            return emptyList()
        }

        return detailElem.children().drop(4).chunked(4).mapNotNull { (titleElem, detailElem) ->
            try {
                val funId = titleElem.attr("id") ?: titleElem.attr("name") ?: return@mapNotNull null
                val (name, params) = parseFunSignature(funId)

                val detail = detailElem.child(0)
                val isStatic = detail.child(1).wholeText().split("\\W+".toRegex()).contains("static")

                val info = FunctionInfo(
                        file = file,
                        name = name,
                        isStatic = isStatic,
                        paramTypes = params
                )

                val doc = FunctionDoc(
                        link = null,
                        paramNames = null,
                        shortInfo = null,
                        longInfo = detail.child(2).wholeText()
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
                        (packageTest in packages) && name.endsWith(".html")
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