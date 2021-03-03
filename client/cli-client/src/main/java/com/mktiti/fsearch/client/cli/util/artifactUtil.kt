package com.mktiti.fsearch.client.cli.util

import com.mktiti.fsearch.dto.ArtifactIdDto

fun parseArtifactId(id: String): ArtifactIdDto? {
    val parts = id.split(":")
    return if (parts.size == 3) {
        ArtifactIdDto(parts[0], parts[1], parts[2])
    } else {
        null
    }
}