package com.mktiti.fsearch.rest.api

import javax.servlet.ServletRequest

private const val loggedUserAttrib = "logged_user"

fun ServletRequest.setLoggedUser(user: User) {
    setAttribute(loggedUserAttrib, user)
}

fun ServletRequest.loggedUser(): User {
    return safeLoggedUser()!!
}

fun ServletRequest.safeLoggedUser(): User? {
    return getAttribute(loggedUserAttrib) as? User
}
