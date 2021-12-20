package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.auth.AuthenticationService
import com.mktiti.fsearch.backend.auth.JwtService
import com.mktiti.fsearch.backend.info.InfoService
import com.mktiti.fsearch.backend.search.SearchService
import com.mktiti.fsearch.backend.stats.StatisticService
import com.mktiti.fsearch.backend.user.UserService
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import com.mktiti.fsearch.rest.api.handler.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DefaultHandlerConfig(
        searchService: SearchService,
        authService: AuthenticationService,
        infoService: InfoService,
        jwtService: JwtService,
        userService: UserService,
        statService: StatisticService
) {

    private val authHandler: AuthHandler = BasicAuthHandler(authService, jwtService)
    private val searchHandler: SearchHandler = BasicSearchHandler(searchService, BasicFitPresenter.default(MapJavaInfoRepo), statService)
    private val infoHandler: InfoHandler = BasicInfoHandler(infoService)
    private val artifactHandler: ArtifactHandler = BasicArtifactHandler(infoService)
    private val userHandler: UserHandler = BasicUserHandler(userService)
    private val adminHandler: AdminHandler = BasicAdminHandler(statService, userService)

    @Bean fun defaultSearchHandler(): SearchHandler = searchHandler
    @Bean fun defaultAuthHandler(): AuthHandler = authHandler
    @Bean fun infoHandler(): InfoHandler = infoHandler
    @Bean fun artifactHandler(): ArtifactHandler = artifactHandler
    @Bean fun userHandler(): UserHandler = userHandler
    @Bean fun adminHandler(): AdminHandler = adminHandler

}
