package com.mktiti.fsearch.backend.stats

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringStatsConfiguration {

    @Bean
    fun statsService(): StatisticService = InMemStatService(20)

}