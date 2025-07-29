#!/bin/sh

set -ex

./gradlew goJF

if [ -n "$(git status --porcelain)" ]; then
  echo "There are changes after linting (used goJF) cmd: ./gradlew goJF"
  echo "Please rerun the command and commit the changes"
  git status --porcelain
  exit 1
fi