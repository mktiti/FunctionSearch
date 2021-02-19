package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.backend.api.InfoHandler
import com.mktiti.fsearch.backend.api.SearchHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.api")
@Profile("nop")
class NopHandlerBean {

    @Bean
    fun searchHandler(): SearchHandler = SearchHandler.Nop

    @Bean
    fun infoHandler(): InfoHandler = InfoHandler.Nop

    @Bean
    fun artifactHandler(): ArtifactHandler = ArtifactHandler.Nop

}
