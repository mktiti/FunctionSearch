package com.mktiti.fsearch.backend.user

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.annotation.Validated
import java.time.LocalDate
import javax.validation.constraints.NotBlank

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "users")
data class UserConfig(
        val testUsers: List<TestUser>?
)

data class TestUser(
        @NotBlank val username: String,
        val level: Level = Level.NORMAL,
        @DateTimeFormat(pattern = "yyyy-MM-dd") val registerDate: LocalDate
)
