package com.mktiti.fsearch.backend.spring.controller

import com.mktiti.fsearch.backend.api.ArtifactHandler
import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.rest.api.controller.ArtifactController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "artifact")
class SpringArtifactController @Autowired constructor(private val backingHandler: ArtifactHandler) : ArtifactController {

    override fun artifacts(): ResultList<ArtifactIdDto> = backingHandler.all()

    override fun byGroup(group: String): ResultList<ArtifactIdDto> = backingHandler.byGroup(group)

    override fun byName(group: String, name: String): ResultList<ArtifactIdDto> = backingHandler.byName(group, name)

    override fun get(group: String, name: String, version: String): ArtifactIdDto? = backingHandler.get(group, name, version)

    override fun remove(
            group: String,
            name: String,
            version: String,
            response: HttpServletResponse
    ): MessageDto {
        return MessageDto(if (backingHandler.remove(group, name, version)) {
            "OK"
        } else {
            response.status = HttpServletResponse.SC_NOT_FOUND
            "NOT FOUND"
        })
    }

    override fun load(artifactIdDto: ArtifactIdDto) {
        backingHandler.create(artifactIdDto)
    }

}