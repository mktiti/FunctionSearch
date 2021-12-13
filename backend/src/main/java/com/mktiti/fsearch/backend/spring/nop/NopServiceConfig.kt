package com.mktiti.fsearch.backend.spring.nop

import com.mktiti.fsearch.backend.auth.AuthenticationService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan("com.mktiti.fsearch.backend.auth")
@Profile("nop")
class NopServiceConfig {

    @Bean
    fun nopAuthService(): AuthenticationService = AuthenticationService.Nop

}