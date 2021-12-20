package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.rest.api.AnyLoginRequired
import com.mktiti.fsearch.rest.api.handler.ArtifactHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "artifact")
class ArtifactController(private val handler: ArtifactHandler) {

    @GetMapping("/artifacts")
    @ResponseBody
    fun artifacts(): ResultList<ArtifactIdDto> = handler.all()

    @GetMapping("/artifacts/{group}")
    @ResponseBody
    fun byGroup(@PathVariable("group") group: String): ResultList<ArtifactIdDto> = handler.byGroup(group)

    @GetMapping("/artifacts/{group}/{name}")
    @ResponseBody
    fun byName(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String
    ): ResultList<ArtifactIdDto> = handler.byName(group, name)

    @GetMapping("/artifacts/{group}/{name}/{version}")
    @ResponseBody
    fun get(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String
    ): ArtifactIdDto? = handler.get(group, name, version)

    @DeleteMapping("/artifacts/{group}/{name}/{version}")
    @ResponseBody
    fun remove(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String,
            response: HttpServletResponse
    ): MessageDto {
        return MessageDto(if (handler.remove(group, name, version)) {
            "OK"
        } else {
            response.status = HttpServletResponse.SC_NOT_FOUND
            "NOT FOUND"
        })
    }

    @PostMapping("/artifacts")
    @ResponseBody
    @AnyLoginRequired
    fun load(@RequestBody id: ArtifactIdDto) = handler.create(id)

}