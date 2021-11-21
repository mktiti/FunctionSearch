package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.BasicArtifactHandler
import com.mktiti.fsearch.backend.BasicFitPresenter
import com.mktiti.fsearch.backend.BasicInfoHandler
import com.mktiti.fsearch.backend.BasicSearchHandler
import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.backend.api.InfoHandler
import com.mktiti.fsearch.backend.api.SearchHandler
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.api")
@Profile("default")
class DefaultHandlerBean(
        @Value("\${data-store.path:#{null}}") private val storeBasePathConfig: String?,
        @Value("\${jcl.javadoc.path:#{null}}") private val javadocPath: String?,
        @Value("\${cache.docs.approxlimit}") private val docsCacheLimit: Int,
        @Value("\${cache.info.approxlimit}") private val infoCacheLimit: Int,
) {

    @PostConstruct
    fun initializeContext() {
        val storeBase: Path = if (storeBasePathConfig == null) {
            Files.createTempDirectory("fsearch-store-")
        } else {
            Paths.get(storeBasePathConfig)
        }

        println("Initializing (Data store: $storeBase, JCL docs: $javadocPath)")
        ContextManagerStore.init(
                storeRoot = storeBase,
                jclDocLocation = javadocPath?.let(Paths::get),
                infoCacheLimit = infoCacheLimit,
                docsCacheLimit = docsCacheLimit
        )
    }

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
