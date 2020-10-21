package com.mktiti.fsearch.maven.repo

import com.mktiti.fsearch.maven.MavenArtifact
import com.mktiti.fsearch.util.indexOfOrNull
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

interface DependencyResolver {

    fun dependencies(pomFile: File): Collection<MavenArtifact>?

    fun dependencies(pomStream: InputStream): Collection<MavenArtifact>?

}

// TODO
object PrimitiveDependencyResolver : DependencyResolver {

    private data class Dependency(
            val artifact: MavenArtifact,
            val scope: String
    )

    private val propertiesXpath = XPathFactory.newInstance().newXPath().compile("/project/properties")
    private val dependenciesXpath = XPathFactory.newInstance().newXPath().compile("/project/dependencies/dependency")

    private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }

    // TODO very primitive
    private tailrec fun escapeValue(value: String, properties: Map<String, String>): String? {
        val dollarPos = value.indexOfOrNull("\${") ?: return value
        val endPos = value.indexOfOrNull("}", startIndex = dollarPos) ?: return null

        val pre = value.substring(0, dollarPos)
        val post = value.substring(endPos, value.length)
        val variable = value.substring(dollarPos + 2, endPos)
        val resolved = properties[variable] ?: return null

        val newVal = "$pre$resolved$post"
        return escapeValue(newVal, properties)
    }

    // TODO very primitive
    private fun mapDependency(elem: Element, properties: Map<String, String>): Dependency? {
        val children = elem.childNodes.toList().mapNotNull { child ->
            (child as? Element)?.let { it.tagName to it.textContent }
        }
        fun tag(tag: String): String? {
            val rawVal = children.find { it.first == tag }?.second ?: return null
            return escapeValue(rawVal, properties)
        }

        val group = tag("groupId") ?: return null
        val artifact = tag("artifactId") ?: return null
        val version = tag("version") ?: "1.0.0"

        val info = MavenArtifact(
                group = group.split('.'),
                name = artifact,
                version = version
        )

        return Dependency(
                info, tag("scope") ?: "compile"
        )
    }

    override fun dependencies(pomStream: InputStream): Collection<MavenArtifact>? {
        return with(DocumentBuilderFactory.newInstance().newDocumentBuilder()) {
            val document = parse(pomStream) ?: return null

            val properties = (propertiesXpath.evaluate(document, XPathConstants.NODESET) as? NodeList)
                    ?.toList()
                    ?.filterIsInstance<Element>()
                    ?.map { (it.tagName ?: "") to (it.textContent ?: "") }
                    ?.toMap() ?: emptyMap()

            (dependenciesXpath.evaluate(document, XPathConstants.NODESET) as? NodeList)
                    ?.toList()
                    ?.filterIsInstance<Element>()
                    ?.mapNotNull { mapDependency(it, properties) }
                    ?.filter { it.scope != "test" }
                    ?.map { it.artifact }
        }
    }

    override fun dependencies(pomFile: File): Collection<MavenArtifact>? {
        return FileInputStream(pomFile).use { inputStream ->
            dependencies(inputStream)
        }
    }

}
