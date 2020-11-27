import com.mktiti.fsearch.frontend.ContextHandler
import com.mktiti.fsearch.frontend.SearchHandler
import kotlinx.browser.document
import org.w3c.dom.*

fun main() {
    fun elemById(id: String) = document.getElementById(id) ?: error("Elem #$id not found")
    fun divById(id: String) = document.getElementById(id) as? HTMLDivElement ?: error("Elem #$id not found")

    val contextHandler = ContextHandler(
            contextElem = divById("context-select"),
            header = elemById("context-summary") as HTMLElement,
            summaryElem = elemById("summary-text") as HTMLSpanElement,
            artifactsElem = divById("context-listing"),
            newArtifactInput = elemById("artifact-add-name") as HTMLInputElement,
            addArtifact = elemById("artifact-add") as HTMLButtonElement
    )

    SearchHandler(
            searchBar = elemById("search-bar") as HTMLInputElement,
            searchButton = elemById("search-submit") as HTMLButtonElement,
            resultOk = divById("result-ok"),
            resultList = divById("result-list"),
            errorDiv = divById("result-error"),
            resultWrap = divById("result-wrap"),
            loadingMessageDiv = divById("result-load"),
            contextProvider = contextHandler::context
    )
}