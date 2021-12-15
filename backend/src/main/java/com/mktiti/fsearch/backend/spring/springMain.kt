@file:JvmName("SpringStart")

package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.api.grpc.GrpcArtifactService
import com.mktiti.fsearch.backend.api.grpc.GrpcAuthService
import com.mktiti.fsearch.backend.api.grpc.GrpcInfoService
import com.mktiti.fsearch.backend.api.grpc.GrpcSearchService
import com.mktiti.fsearch.backend.api.rest.SpringArtifactController
import com.mktiti.fsearch.backend.api.rest.SpringAuthController
import com.mktiti.fsearch.backend.api.rest.SpringInfoController
import com.mktiti.fsearch.backend.api.rest.SpringSearchController
import com.mktiti.fsearch.backend.auth.AuthConfig
import com.mktiti.fsearch.backend.cache.CacheConfig
import com.mktiti.fsearch.core.cache.CentralInfoCache
import com.mktiti.fsearch.core.cache.CleaningInternCache
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(
        GrpcSearchService::class, GrpcInfoService::class, GrpcArtifactService::class, GrpcAuthService::class,
        SpringSearchController::class, SpringInfoController::class, SpringArtifactController::class, SpringAuthController::class
)
@EnableConfigurationProperties(AuthConfig::class, CacheConfig::class)
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
