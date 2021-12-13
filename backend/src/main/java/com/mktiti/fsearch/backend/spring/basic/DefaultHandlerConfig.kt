package com.mktiti.fsearch.backend.spring.basic

import com.mktiti.fsearch.backend.auth.AuthenticationService
import com.mktiti.fsearch.backend.handler.ArtifactHandler
import com.mktiti.fsearch.backend.handler.AuthHandler
import com.mktiti.fsearch.backend.handler.InfoHandler
import com.mktiti.fsearch.backend.handler.SearchHandler
import com.mktiti.fsearch.backend.handler.basic.*
import com.mktiti.fsearch.backend.info.InfoService
import com.mktiti.fsearch.backend.search.SearchService
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.handler")
class DefaultHandlerConfig(
        searchService: SearchService,
        authService: AuthenticationService,
        infoService: InfoService
) {

    private val authHandler: AuthHandler = BasicAuthHandler(authService)
    private val searchHandler: SearchHandler = BasicSearchHandler(searchService, BasicFitPresenter.default(MapJavaInfoRepo))
    private val infoHandler: InfoHandler = BasicInfoHandler(infoService)
    private val artifactHandler: ArtifactHandler = BasicArtifactHandler(infoService)

    @Bean fun defaultSearchHandler(): SearchHandler = searchHandler
    @Bean fun defaultAuthHandler(): AuthHandler = authHandler
    @Bean fun infoHandler(): InfoHandler = infoHandler
    @Bean fun artifactHandler(): ArtifactHandler = artifactHandler

}
