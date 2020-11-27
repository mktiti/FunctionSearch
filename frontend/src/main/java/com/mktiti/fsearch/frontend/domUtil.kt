package com.mktiti.fsearch.frontend

import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLElement

fun HTMLElement.toggleClass(cssClass: String): Boolean = if (removeClass(cssClass)) {
    false
} else {
    addClass(cssClass)
    true
}

fun HTMLElement.removeChildren(leave: Int = 0) {
    while (children.length > leave) {
        children.item(0)?.let { child ->
            removeChild(child)
        }
    }
}
