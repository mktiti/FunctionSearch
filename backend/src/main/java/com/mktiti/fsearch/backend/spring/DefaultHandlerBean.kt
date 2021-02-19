package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.BasicArtifactHandler
import com.mktiti.fsearch.backend.BasicFitPresenter
import com.mktiti.fsearch.backend.BasicInfoHandler
import com.mktiti.fsearch.backend.BasicSearchHandler
import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.backend.api.InfoHandler
import com.mktiti.fsearch.backend.api.SearchHandler
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.api")
@Profile("default")
class DefaultHandlerBean {

    @Bean
    fun defaultSearchHandler(): SearchHandler = BasicSearchHandler(
            contextManager = ContextManagerStore.contextManager,
            fitPresenter = BasicFitPresenter.default(MapJavaInfoRepo)
    )

    @Bean
    fun infoHandler(): InfoHandler = BasicInfoHandler(
            contextManager = ContextManagerStore.contextManager
    )

    @Bean
    fun artifactHandler(): ArtifactHandler = BasicArtifactHandler(
            artifactManager = ContextManagerStore.artifactManager,
            contextManager = ContextManagerStore.contextManager
    )

}
