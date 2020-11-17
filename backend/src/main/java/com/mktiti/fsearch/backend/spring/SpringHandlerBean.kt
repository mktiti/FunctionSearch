package com.mktiti.fsearch.backend.spring

import com.mktiti.fsearch.backend.BasicSearchHandler
import com.mktiti.fsearch.backend.api.SearchHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.api")
class SpringHandlerBean {

    @Bean
    fun handler(): SearchHandler = BasicSearchHandler(ContextManagerStore.contextManager)

}