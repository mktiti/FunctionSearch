package com.mktiti.fsearch.backend.user

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringUserConfiguration {

    @Bean
    fun userService(config: UserConfig): UserService = TestUserService(config.testUsers ?: emptyList())

}