#!/usr/bin/env bash

readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly NC='\033[0m'

printColor() {
  local -r color=${1}
  local -r message=${2}
  echo -e "$color${message}${NC}"
}

printError() {
  printColor "$RED" "$1"
}

failError() {
  printError "$1"
  exit 1
}

printSuccess() {
  printColor "$GREEN" "$1"
}

readonly imageDir="$1/images"
readonly configDir="$1/config"

mkdir -p "$configDir/backend"
mkdir -p "$configDir/frontend-vue"
mkdir -p "$configDir/facade-docker"

readonly backendImage="$imageDir/backend.tar.gz"
readonly frontendImage="$imageDir/frontend.tar.gz"
readonly facadeImage="$imageDir/facade.tar.gz"

if [[ -f "$backendImage" && -f "$frontendImage" &&  -f "$facadeImage" ]]
then
  echo "Loading docker images [$backendImage, $frontendImage, $facadeImage]"
else
  failError "Some/One of the docker image ($backendImage, $frontendImage, $facadeImage) files does not exist!"
fi

readonly facadeUsage=${2}
if [[ -z "$facadeUsage" ]]; then
  failError "Second argument [facade/noFacade] missing!"
elif [[ "$facadeUsage" != "facade" && "$facadeUsage" != "noFacade" ]]; then
  failError "Invalid second argument [facade/noFacade]!"
fi

loadDockerImage() {
  docker load -i "$1" || failError "Failed to load docker image ($1)"
}

loadDockerImage "$backendImage"
loadDockerImage "$frontendImage"
loadDockerImage "$facadeImage"

cd "$configDir" || failError "Failed to cd into config dir $1"

docker-compose down
printSuccess "Previous deployment down"

if [[ "$facadeUsage" == "facade" ]]; then
  docker-compose up -d --no-recreate --no-build || failError "Failed to start docker [*] deploy"
else
  docker-compose up -d --no-recreate --no-build backend frontend || failError "Failed to start docker [backend, frontend] deploy"
fi

printSuccess "Deployment done"