package com.mktiti.fsearch.backend.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringAuthConfiguration {

    @Bean
    fun jwtService(config: AuthConfig): JwtService = SimpleJwtService(config.jwt)

    @Bean
    fun authService(config: AuthConfig): AuthenticationService = TestAuthService(config)

}
