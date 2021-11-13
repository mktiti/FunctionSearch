package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

interface ArtifactController {

    @RequestMapping("/artifacts", method = [RequestMethod.GET])
    @ResponseBody
    fun artifacts(): ResultList<ArtifactIdDto>

    @RequestMapping("/artifacts/{group}", method = [RequestMethod.GET])
    @ResponseBody
    fun byGroup(@PathVariable("group") group: String): ResultList<ArtifactIdDto>

    @RequestMapping("/artifacts/{group}/{name}", method = [RequestMethod.GET])
    @ResponseBody
    fun byName(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String
    ): ResultList<ArtifactIdDto>

    @RequestMapping("/artifacts/{group}/{name}/{version}", method = [RequestMethod.GET])
    @ResponseBody
    fun get(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String
    ): ArtifactIdDto?

    @RequestMapping("/artifacts/{group}/{name}/{version}", method = [RequestMethod.DELETE])
    @ResponseBody
    fun remove(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String,
            response: HttpServletResponse
    ): MessageDto

    @RequestMapping("/artifacts",  method = [RequestMethod.POST])
    @ResponseBody
    fun load(@RequestBody artifactIdDto: ArtifactIdDto)

}