import {ArtifactIdDto} from "fsearch_client";

export function artifactEqual(a: ArtifactIdDto, b: ArtifactIdDto): boolean {
    return a.group === b.group && a.name === b.name && a.version === b.version
}
