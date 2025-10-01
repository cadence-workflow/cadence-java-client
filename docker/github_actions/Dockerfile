# Always use a release version or a specific commit hash below. Don't use
# a branch name since we'd keep getting updates as long as there are new
# commits to that branch and one of them can break the build
FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine

# Install dependencies using apk
RUN apk update && apk add --virtual wget ca-certificates wget
# Git is needed in order to update the dls submodule
RUN apk add git libstdc++ bash curl

RUN mkdir /cadence-java-client
WORKDIR /cadence-java-client
