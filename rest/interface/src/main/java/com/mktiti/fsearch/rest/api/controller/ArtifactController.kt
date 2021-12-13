package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

interface ArtifactController {

    @GetMapping("/artifacts")
    @ResponseBody
    fun artifacts(): ResultList<ArtifactIdDto>

    @GetMapping("/artifacts/{group}")
    @ResponseBody
    fun byGroup(@PathVariable("group") group: String): ResultList<ArtifactIdDto>

    @GetMapping("/artifacts/{group}/{name}")
    @ResponseBody
    fun byName(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String
    ): ResultList<ArtifactIdDto>

    @GetMapping("/artifacts/{group}/{name}/{version}")
    @ResponseBody
    fun get(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String
    ): ArtifactIdDto?

    @DeleteMapping("/artifacts/{group}/{name}/{version}")
    @ResponseBody
    fun remove(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String,
            response: HttpServletResponse
    ): MessageDto

    @PostMapping("/artifacts")
    @ResponseBody
    fun load(@RequestBody artifactIdDto: ArtifactIdDto)

}