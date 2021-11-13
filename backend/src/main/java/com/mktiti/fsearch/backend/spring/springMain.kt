@file:JvmName("SpringStart")

package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.ProjectInfo
import com.mktiti.fsearch.backend.grpc.GrpcArtifactService
import com.mktiti.fsearch.backend.grpc.GrpcInfoService
import com.mktiti.fsearch.backend.grpc.GrpcSearchService
import com.mktiti.fsearch.core.cache.CentralInfoCache
import com.mktiti.fsearch.core.cache.CleaningInternCache
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(GrpcSearchService::class, GrpcInfoService::class, GrpcArtifactService::class)
class SpringMain {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().components(Components()).info(Info().apply {
        title = "FunctionSearch"
        version = ProjectInfo.version.removeSuffix("-SNAPSHOT")
    })

}

fun main(args: Array<String>) {
    CentralInfoCache.setCleanableCache(CleaningInternCache())
    SpringApplication.run(SpringMain::class.java, *args)
}
