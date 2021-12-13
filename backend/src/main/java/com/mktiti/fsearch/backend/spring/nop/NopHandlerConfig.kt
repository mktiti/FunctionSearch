package com.mktiti.fsearch.backend.spring.nop

import com.mktiti.fsearch.backend.handler.ArtifactHandler
import com.mktiti.fsearch.backend.handler.InfoHandler
import com.mktiti.fsearch.backend.handler.SearchHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.handler")
@Profile("nop")
class NopHandlerConfig {

    @Bean
    fun searchHandler(): SearchHandler = SearchHandler.Nop

    @Bean
    fun infoHandler(): InfoHandler = InfoHandler.Nop

    @Bean
    fun artifactHandler(): ArtifactHandler = ArtifactHandler.Nop

}
