package com.mktiti.fsearch.rest.api.controller.nop

import com.mktiti.fsearch.dto.ArtifactIdDto
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.ResultList
import com.mktiti.fsearch.rest.api.controller.ArtifactController
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "artifact")
class NopArtifactController : ArtifactController {

    override fun artifacts(): ResultList<ArtifactIdDto> = nop()

    override fun byGroup(group: String): ResultList<ArtifactIdDto> = nop()

    override fun byName(group: String, name: String): ResultList<ArtifactIdDto> = nop()

    override fun get(group: String, name: String, version: String): ArtifactIdDto? = nop()

    override fun remove(group: String, name: String, version: String, response: HttpServletResponse): MessageDto = nop()

    override fun load(artifactIdDto: ArtifactIdDto) {}

}