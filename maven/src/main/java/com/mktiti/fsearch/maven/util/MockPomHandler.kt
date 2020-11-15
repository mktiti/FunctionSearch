package com.mktiti.fsearch.maven.util

import com.mktiti.fsearch.modules.ArtifactId
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

internal object MockPomHandler {

    private const val pomTemplateLoc = "/test-pom.xml"

    private val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val template: Document
    private val dependenciesXpath: XPathExpression = XPathFactory.newInstance().newXPath().compile("/project/dependencies")

    private val transformer: Transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        setOutputProperty(OutputKeys.METHOD, "xml")
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
    }

    init {
        template = MockPomHandler::class.java.getResourceAsStream(pomTemplateLoc).use { templateStream ->
            builder.parse(templateStream) ?: error("Failed to parse mock pom.xml template!")
        }
    }

    fun createMockPom(dependencies: Collection<ArtifactId>, output: OutputStream) {
        with(builder.newDocument()) {
            appendChild(importNode(template.documentElement, true))
            val depsElem = dependenciesXpath.evaluate(this, XPathConstants.NODE) as Element

            dependencies.forEach { dependency ->
                val depElem = createElement("dependency")
                fun elem(tag: String, value: String): Element = createElement(tag).apply {
                    appendChild(createTextNode(value))
                    depElem.appendChild(this)
                }

                elem("groupId", dependency.group.joinToString(separator = "."))
                elem("artifactId", dependency.name)
                elem("version", dependency.version)

                depsElem.appendChild(depElem)
            }

            transformer.transform(DOMSource(this), StreamResult(OutputStreamWriter(output, StandardCharsets.UTF_8)))
        }
    }

}