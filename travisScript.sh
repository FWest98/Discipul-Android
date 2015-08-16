#!/bin/bash
set -ev
./gradlew :RoosterPGPlus:clean :RoosterPGPlus:assembleRelease
if [ -z "${TRAVIS_TAG+x}" ]; then
    ./gradlew :RoosterPGPlus:publishApkRelease
fi