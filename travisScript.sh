#!/bin/bash
set -ev
./gradlew :RoosterPGPlus:clean :RoosterPGPlus:assembleRelease
if [ "#{TRAVIS_TAG}" = "true" ]; then
    ./gradlew :RoosterPGPlus:publishApkRelease
fi