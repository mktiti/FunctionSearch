package com.mktiti.fsearch.backend.spring.handler

import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "artifact")
class SpringArtifactHandler @Autowired constructor(private val backingHandler: ArtifactHandler) {

    @RequestMapping("/artifacts", method = [RequestMethod.GET])
    @ResponseBody
    fun artifacts(): ResultList<ArtifactIdDto> = backingHandler.all()

    @RequestMapping("/artifacts/{group}", method = [RequestMethod.GET])
    @ResponseBody
    fun byGroup(@PathVariable("group") group: String): ResultList<ArtifactIdDto> = backingHandler.byGroup(group)

    @RequestMapping("/artifacts/{group}/{name}", method = [RequestMethod.GET])
    @ResponseBody
    fun byName(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String
    ): ResultList<ArtifactIdDto> = backingHandler.byName(group, name)

    @RequestMapping("/artifacts/{group}/{name}/{version}", method = [RequestMethod.GET])
    @ResponseBody
    fun get(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String
    ): ArtifactIdDto? = backingHandler.get(group, name, version)

    @RequestMapping("/artifacts/{group}/{name}/{version}", method = [RequestMethod.DELETE])
    @ResponseBody
    fun remove(
            @PathVariable("group") group: String,
            @PathVariable("name") name: String,
            @PathVariable("version") version: String,
            response: HttpServletResponse
    ): MessageDto {
        return MessageDto(if (backingHandler.remove(group, name, version)) {
            "OK"
        } else {
            response.status = HttpServletResponse.SC_NOT_FOUND
            "NOT FOUND"
        })
    }

    @RequestMapping("/artifacts",  method = [RequestMethod.POST])
    @ResponseBody
    fun load(@RequestBody artifactIdDto: ArtifactIdDto) {
        backingHandler.create(artifactIdDto)
    }

}