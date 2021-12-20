package com.mktiti.fsearch.backend.auth

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent.Invalid.BadFormat
import com.mktiti.fsearch.backend.auth.JwtService.JwtContent.Invalid.Expired
import com.mktiti.fsearch.rest.api.*
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.filter.ServletRequestPathFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.Method
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class JwtFilterConf(
        private val jwtService: JwtService,
        private val reqMap: RequestMappingHandlerMapping
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()

        http.addFilterBefore(ServletRequestPathFilter(), BasicAuthenticationFilter::class.java)
        http.addFilterAfter(JwtFiler(jwtService, reqMap), BasicAuthenticationFilter::class.java)
    }

}

private data class AuthContext<A : Annotation>(
        val annotation: A,
        val user: User?
)

private typealias AnnotationAuth<A> = AuthContext<A>.() -> Boolean

private typealias Authorizer = (User?) -> Boolean

private data class AuthEntry<A : Annotation>(
        val annotationClass: Class<A>,
        val validator: AnnotationAuth<A>
) {

    fun forMethod(method: Method): Authorizer? = method.getAnnotation(annotationClass)?.let { annotation ->
        { user -> validator(AuthContext(annotation, user)) }
    }

}

class JwtFiler(
        private val jwtService: JwtService,
        private val reqMap: RequestMappingHandlerMapping
) : OncePerRequestFilter() {

    companion object {
        private const val bearerPrefix = "Bearer "

        private val filteredMethods = listOf(GET, PUT, POST, DELETE, PATCH).map {
            it.name
        }

        private val annotationLogic = listOf<AuthEntry<*>>(
                AuthEntry(NoLoginOnly::class.java) { user == null },
                AuthEntry(AnyLoginRequired::class.java) { user != null },
                AuthEntry(UserOnly::class.java) { user?.role == Role.USER },
                AuthEntry(AdminOnly::class.java) { user?.role == Role.ADMIN },
                AuthEntry(LoginRequired::class.java) { user?.role in annotation.roles },
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.method !in filteredMethods
    }

    private fun targetMethod(request: HttpServletRequest): Method {
        return (reqMap.getHandler(request)?.handler as? HandlerMethod?)?.method ?: error("Failed to find handler method for HTTP request")
    }

    override fun getFilterName() = "JWT filter"

    private fun jwtUser(request: HttpServletRequest): Either<String, User?> {
        val jwtString = request.getHeader(AUTHORIZATION)?.removePrefix(bearerPrefix)?.trim()

        return if (jwtString == null || jwtString.isBlank()) {
            Either.Right(null)
        } else {
            when (val jwt = jwtService.parse(jwtString)) {
                is JwtContent.Valid -> jwt.user.right()
                is BadFormat -> "Malformed JWT".left()
                is Expired -> "Expired JWT".left()
            }
        }
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val targetMethod = targetMethod(request)

        val toAuthorize: List<Authorizer> = annotationLogic.mapNotNull { entry ->
            entry.forMethod(targetMethod)
        }

        if (toAuthorize.isNotEmpty()) {
            val authorizedOrMsg = jwtUser(request).flatMap { user ->
                val authorized = toAuthorize.all { authorize -> authorize(user) }
                if (authorized) user.right() else "Not authorized".left()
            }

            when (authorizedOrMsg) {
                is Either.Right -> {
                    authorizedOrMsg.value?.let(request::setLoggedUser)
                }
                is Either.Left -> {
                    response.status = UNAUTHORIZED.value()
                    response.writer.write("Authentication error - ${authorizedOrMsg.value}")
                    return
                }
            }
        }

        chain.doFilter(request, response)
    }

}