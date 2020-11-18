import com.mktiti.fsearch.frontend.SearchHandler
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement

fun main() {
    fun elemById(id: String) = document.getElementById(id) ?: error("Elem #$id not found")
    fun divById(id: String) = document.getElementById(id) as? HTMLDivElement ?: error("Elem #$id not found")

    SearchHandler(
            searchBar = elemById("search-bar") as HTMLInputElement,
            searchButton = elemById("search-submit") as HTMLButtonElement,
            resultOk = divById("result-ok"),
            resultList = divById("result-list"),
            errorDiv = divById("result-error"),
            resultWrap = divById("result-wrap"),
            loadingMessageDiv = divById("result-load")
    )
}