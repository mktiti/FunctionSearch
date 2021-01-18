package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.BasicFitPresenter
import com.mktiti.fsearch.backend.BasicSearchHandler
import com.mktiti.fsearch.backend.api.SearchHandler
import com.mktiti.fsearch.core.repo.MapJavaInfoRepo
import org.springframework.context.annotation.*

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.api")
class SpringHandlerBean {

    @Bean
    @Profile("default")
    fun defaultHandler(): SearchHandler = BasicSearchHandler(
            contextManager = ContextManagerStore.contextManager,
            fitPresenter = BasicFitPresenter.default(MapJavaInfoRepo)
    )

    @Bean
    @Profile("nop")
    fun nopHandler(): SearchHandler = SearchHandler.Nop

}
