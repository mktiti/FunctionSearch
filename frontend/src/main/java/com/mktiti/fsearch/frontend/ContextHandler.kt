package com.mktiti.fsearch.frontend

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.*
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent

class ContextHandler(
        private val contextElem: HTMLElement,
        header: HTMLElement,
        private val summaryElem: HTMLSpanElement,
        private val artifactsElem: HTMLDivElement,
        private val newArtifactInput: HTMLInputElement,
        addArtifact: HTMLButtonElement,
        initContext: QueryContext = defaultContext
) {

    companion object {
        private const val headerMinClass = "min"

        val defaultContext = QueryContext(
                artifacts = listOf(
                        ArtifactId("org.apache.commons", "commons-lang3", "3.11"),
                        ArtifactId("com.google.guava", "guava", "30.0-jre")
                )
        )

        private fun searchInString(count: Int) = when (count) {
            0 -> "Search only in standard library"
            1 -> "Search in one artifact"
            else -> "Search in $count artifacts"
        }
    }

    private var storedContext = initContext
    val context: QueryContext
        get() = storedContext

    init {
        (header as? EventTarget)?.addEventListener("click", { contextElem.toggleClass(headerMinClass) }, false)

        val enterListener = EventListener { event ->
            if ((event as? KeyboardEvent)?.keyCode == 13) {
                event.preventDefault()
                onAddArtifact()
            }
        }
        (newArtifactInput as? EventTarget)?.addEventListener("keyup", enterListener, false)
        (addArtifact as? EventTarget)?.addEventListener("click", { onAddArtifact() }, false)

        update()
    }

    private fun onAddArtifact() {
        val input = newArtifactInput.value.trim()

        val parts = input.split(":", limit = 4)
        if (parts.size == 3) {
            val (group, name, version) = parts
            storedContext += ArtifactId(group, name, version)
            newArtifactInput.value = ""
            update()
        } else {
            window.alert("Invalid artifact id '$input'")
        }
    }

    private fun artifactRemover(artifactId: ArtifactId): () -> Unit = {
        storedContext -= artifactId
        update()
    }

    private fun mapArtifact(artifactId: ArtifactId): HTMLElement {
        return (document.createElement("div") as HTMLDivElement).apply {
            val removeButton = (document.createElement("button") as HTMLButtonElement).apply {
                addClass("fa", "fa-trash")
                val listener = artifactRemover(artifactId)
                (this as EventTarget).addEventListener("click", { listener() }, false)
            }
            appendChild(removeButton)

            addClass("artifact-item")

            val textElem = (document.createElement("span") as HTMLSpanElement).apply {
                textContent = "${artifactId.group}:${artifactId.name}:${artifactId.version}"
            }
            appendChild(textElem)
        }
    }

    private fun update() = with(context) {
        summaryElem.innerText = searchInString(artifacts.size)
        artifactsElem.removeChildren()

        artifacts.map(this@ContextHandler::mapArtifact).forEach(artifactsElem::appendChild)
    }

}
