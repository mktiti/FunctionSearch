package com.mktiti.fsearch.frontend

import com.mktiti.fsearch.frontend.QueryResult.Error.InternalError
import com.mktiti.fsearch.frontend.QueryResult.Error.Query
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.xhr.XMLHttpRequest

class SearchHandler(
        private val searchBar: HTMLInputElement,
        searchButton: HTMLButtonElement,
        private val resultOk: HTMLDivElement,
        private val resultList: HTMLDivElement,
        private val errorDiv: HTMLDivElement,
        private val resultWrap: HTMLDivElement,
        private val loadingMessageDiv: HTMLElement
) {

    companion object {
        private const val itemMinClass = "item-min"

        val defaultContext = QueryCtxDto(
                artifacts = listOf(
                        ArtifactIdDto("org.apache.commons", "commons-lang3", "3.11"),
                        ArtifactIdDto("com.google.guava", "guava", "30.0-jre")
                )
        )
    }

    private fun onItemCLick(source: HTMLDivElement) {
        if (!source.removeClass(itemMinClass)) {
            source.addClass(itemMinClass)
        }
    }

    init {
        val enterListener = EventListener { event ->
            if ((event as? KeyboardEvent)?.keyCode == 13) {
                event.preventDefault()
                initSearch()
            }
        }
        (searchBar as? EventTarget)?.addEventListener("keyup", enterListener, false)
        (searchButton as? EventTarget)?.addEventListener("click", { initSearch() }, false)
    }

    private fun initSearch() {
        val query = searchBar.value
        if (query.isBlank()) {
            return
        }

        showResult(loadingMessageDiv)

        search(query, defaultContext)
    }

    private fun createItem(fit: QueryFitResult): HTMLDivElement {
        return (document.createElement("div") as HTMLDivElement).apply {
            fun sub(htmlClass: String, content: String) = (document.createElement("div") as HTMLDivElement).let { inner ->
                inner.addClass(htmlClass)
                inner.innerText = content
                appendChild(inner)
            }

            addClass("result-item")
            addClass("item-min")
            sub("file", fit.file)
            sub("header", fit.header)
            sub("short-info", fit.doc.shortInfo ?: "")
            sub("long-info", fit.doc.details ?: "----")

            addEventListener("click", { onItemCLick(this) }, false)
        }
    }

    private fun onSuccess(result: QueryResult.Success) {
        while (resultList.children.length > 0) {
            resultList.children.item(0)?.let { child ->
                resultList.removeChild(child)
            }
        }

        result.results.map {
            createItem(it)
        }.forEach {
            resultList.appendChild(it)
        }

        showResult(resultOk)
    }

    private fun onError(result: QueryResult.Error) {
        val message = when (result) {
            is InternalError -> "Internal error while processing request"
            is Query -> "Failed to parse query '${result.query}'"
        }
        onError(message)
    }

    private fun onError(message: String) {
        errorDiv.innerText = message
        showResult(errorDiv)
    }

    private fun showResult(elem: HTMLElement) {
        with(resultWrap.childNodes) {
            (0 until length).mapNotNull { item(it) }
        }.mapNotNull { it as? HTMLElement }.forEach { child ->
            child.addClass("hide")
        }
        elem.removeClass("hide")
    }

    private fun search(
            query: String,
            context: QueryCtxDto
    ) {
        with(XMLHttpRequest()) {
            open("POST", "/search", true)
            setRequestHeader("Content-Type", "application/json")
            onreadystatechange = {
                if (readyState == XMLHttpRequest.DONE) {
                    try {
                        when (val result: QueryResult = dtoJson.decodeFromString(responseText)) {
                            is QueryResult.Error -> onError(result)
                            is QueryResult.Success -> onSuccess(result)
                        }
                    } catch (error: Exception) {
                        onError(error.message ?: "Failed to parse result")
                    }
                }
            }

            val param = QueryRequestDto(
                    context = context,
                    query = query
            )
            send(dtoJson.encodeToString(param))
        }
    }

}