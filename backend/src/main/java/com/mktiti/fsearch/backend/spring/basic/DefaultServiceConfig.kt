package com.mktiti.fsearch.backend.spring.basic

import com.mktiti.fsearch.backend.auth.AuthConfig
import com.mktiti.fsearch.backend.auth.AuthenticationService
import com.mktiti.fsearch.backend.auth.TestAuthService
import com.mktiti.fsearch.backend.cache.CacheConfig
import com.mktiti.fsearch.backend.context.ContextManagerStore
import com.mktiti.fsearch.backend.info.BasicInfoService
import com.mktiti.fsearch.backend.info.InfoService
import com.mktiti.fsearch.backend.search.BasicSearchService
import com.mktiti.fsearch.backend.search.SearchService
import com.mktiti.fsearch.util.logInfo
import com.mktiti.fsearch.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.search", "com.mktiti.fsearch.backend.auth", "com.mktiti.fsearch.backend.info")
class DefaultServiceConfig(
        @Value("\${data-store.path:#{null}}") private val storeBasePathConfig: String?,
        @Value("\${jcl.javadoc.path:#{null}}") private val javadocPath: String?,
        private val cacheConfig: CacheConfig,
        authConfig: AuthConfig
) {

    private val log = logger()

    private val authService: AuthenticationService = TestAuthService(authConfig)

    private val searchService: SearchService by lazy {
        BasicSearchService(ContextManagerStore.contextManager)
    }

    private val infoService: InfoService by lazy {
        BasicInfoService(
                ContextManagerStore.contextManager,
                ContextManagerStore.artifactManager
        )
    }

    @PostConstruct
    fun initializeContext() {
        val storeBase: Path = if (storeBasePathConfig == null) {
            Files.createTempDirectory("fsearch-store-")
        } else {
            Paths.get(storeBasePathConfig)
        }

        log.logInfo { "Initializing (Data store: $storeBase, JCL docs: $javadocPath)" }
        ContextManagerStore.init(
                storeRoot = storeBase,
                jclDocLocation = javadocPath?.let(Paths::get),
                approxLimits = cacheConfig.approxLimit
        )
    }

    @Bean fun defaultAuthService() = authService
    @Bean fun defaultSearchService() = searchService
    @Bean fun defaultInfoService() = infoService

}