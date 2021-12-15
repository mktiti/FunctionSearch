@file:JvmName("SpringNopStart")

package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.rest.api.InterfaceInfo
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import org.springframework.http.HttpHeaders

private const val jwtBearerName = "bearer-jwt"

@SpringBootApplication
class SpringMain {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
            .components(Components().apply {
                addSecuritySchemes(jwtBearerName, SecurityScheme().apply {
                    type = SecurityScheme.Type.HTTP
                    `in` = SecurityScheme.In.HEADER
                    name = HttpHeaders.AUTHORIZATION
                    scheme = "bearer"
                })
            }).info(Info().apply {
                title = "JvmSearch"
                version = InterfaceInfo.version.removeSuffix("-SNAPSHOT")

            }).security(listOf(
                    SecurityRequirement().addList(jwtBearerName, listOf("read", "write"))
            ))
}

val nopEnv: ConfigurableEnvironment = StandardEnvironment().apply {
    setActiveProfiles("nop")
}

fun main(args: Array<String>) {
    with(SpringApplication(SpringMain::class.java)) {
        setEnvironment(nopEnv)
        run(*args)
    }
}