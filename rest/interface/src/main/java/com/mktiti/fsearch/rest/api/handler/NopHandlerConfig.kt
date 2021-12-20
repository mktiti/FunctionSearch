package com.mktiti.fsearch.rest.api.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("nop")
class NopHandlerConfig {

    @Bean
    fun searchHandler(): SearchHandler = SearchHandler.Nop

    @Bean
    fun infoHandler(): InfoHandler = InfoHandler.Nop

    @Bean
    fun artifactHandler(): ArtifactHandler = ArtifactHandler.Nop

    @Bean
    fun authHandler(): AuthHandler = AuthHandler.Nop

    @Bean
    fun userHandler(): UserHandler = UserHandler.Nop

    @Bean
    fun adminHandler(): AdminHandler = AdminHandler.Nop

}
