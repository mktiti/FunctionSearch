@file:JvmName("SpringNopStart")

package com.mktiti.fsearch.backend.spring

import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment

fun main(args: Array<String>) {
    val nopEnv: ConfigurableEnvironment = StandardEnvironment().apply {
        setActiveProfiles("nop")
    }

    with(SpringApplication(SpringMain::class.java)) {
        setEnvironment(nopEnv)
        run(*args)
    }
}
