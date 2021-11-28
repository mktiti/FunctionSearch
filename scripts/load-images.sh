#!/usr/bin/env bash

imageDir="$1/images"
configDir="$1/config"

backendImage="$imageDir/backend.tar.gz"
frontendImage="$imageDir/frontend.tar.gz"
facadeImage="$imageDir/facade.tar.gz"

if [[ -f "$backendImage" && -f "$frontendImage" &&  -f "$facadeImage" ]]
then
  echo "Loading docker images [$backendImage, $frontendImage, $facadeImage]"
else
  echo "Some/One of the docker image ($backendImage, $frontendImage, $facadeImage) files does not exist!"
  exit 1
fi

docker load -i "$backendImage"
docker load -i "$frontendImage"
docker load -i "$facadeImage"

cd "$configDir" || { echo "Failed to cd into config dir $1"; exit 1; }

docker-compose down
docker-compose up -d --no-recreate --no-build