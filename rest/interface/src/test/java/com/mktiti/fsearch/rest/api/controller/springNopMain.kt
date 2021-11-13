@file:JvmName("SpringNopStart")

package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.rest.api.InterfaceInfo
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringMain {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().components(Components()).info(Info().apply {
        title = "FunctionSearch"
        version = InterfaceInfo.version.removeSuffix("-SNAPSHOT")
    })
}

fun main(args: Array<String>) {
    SpringApplication(SpringMain::class.java).run(*args)
}
