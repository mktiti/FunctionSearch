@file:JvmName("SpringStart")

package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.api.grpc.*
import com.mktiti.fsearch.backend.auth.AuthConfig
import com.mktiti.fsearch.backend.cache.CacheConfig
import com.mktiti.fsearch.backend.user.UserConfig
import com.mktiti.fsearch.core.cache.CentralInfoCache
import com.mktiti.fsearch.core.cache.CleaningInternCache
import com.mktiti.fsearch.rest.api.controller.*
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@Import(
        GrpcSearchService::class, GrpcInfoService::class, GrpcArtifactService::class,
        GrpcAuthService::class, GrpcUserService::class, GrpcAdminService::class,

        SearchController::class, InfoController::class, ArtifactController::class,
        AuthController::class, UserController::class, AdminController::class
)
@EnableConfigurationProperties(AuthConfig::class, CacheConfig::class, UserConfig::class)
@ComponentScan("com.mktiti.fsearch.backend")
class SpringMain {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().components(Components()).info(Info().apply {
        title = "JvmSearch"
        version = ProjectInfo.version.removeSuffix("-SNAPSHOT")
    })

}

fun main(args: Array<String>) {
    CentralInfoCache.setCleanableCache(CleaningInternCache())
    SpringApplication.run(SpringMain::class.java, *args)
}
