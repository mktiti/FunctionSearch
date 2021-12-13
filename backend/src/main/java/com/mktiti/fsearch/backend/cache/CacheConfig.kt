package com.mktiti.fsearch.backend.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.util.unit.DataSize

@ConstructorBinding
@ConfigurationProperties(prefix = "cache")
data class CacheConfig(
    val approxLimit: CacheApproxLimits
)

data class CacheApproxLimits(
        val docs: DataSize,
        val info: DataSize,
        val deps: DataSize
)