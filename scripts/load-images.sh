#!/usr/bin/env bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

printColor() {
  local color=${1}
  local message=${2}
  echo -e "$color${message}${NC}"
}

printError() {
  printColor "$RED" "$1"
}

printSuccess() {
  printColor "$GREEN" "$1"
}

imageDir="$1/images"
configDir="$1/config"

mkdir -p "$configDir/backend"
mkdir -p "$configDir/frontend-vue"
mkdir -p "$configDir/facade-docker"

backendImage="$imageDir/backend.tar.gz"
frontendImage="$imageDir/frontend.tar.gz"
facadeImage="$imageDir/facade.tar.gz"

if [[ -f "$backendImage" && -f "$frontendImage" &&  -f "$facadeImage" ]]
then
  echo "Loading docker images [$backendImage, $frontendImage, $facadeImage]"
else
  printError "Some/One of the docker image ($backendImage, $frontendImage, $facadeImage) files does not exist!"
  exit 1
fi

facadeUsage=${2}
if [[ -z "$facadeUsage" ]]; then
  printError "Second argument [facade/noFacade] missing!"
  exit 1
elif [[ "$facadeUsage" != "facade" && "$facadeUsage" != "noFacade" ]]; then
  printError "Invalid second argument [facade/noFacade]!"
  exit 1
fi

docker load -i "$backendImage"
docker load -i "$frontendImage"
docker load -i "$facadeImage"

cd "$configDir" || {
  printError "Failed to cd into config dir $1"
  exit 1
}

docker-compose down
printSuccess "Previous deployment down"

if [[ "$facadeUsage" == "facade" ]]; then
  docker-compose up -d --no-recreate --no-build || {
    printError "Failed to start docker [*] deploy"
    exit 1
  }
else
  docker-compose up -d --no-recreate --no-build backend frontend || {
    printError "Failed to start docker [backend, frontend] deploy"
    exit 1
  }
fi

printSuccess "Deployment done"