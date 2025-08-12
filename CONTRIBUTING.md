# Developing cadence-java-client

This doc is intended for contributors to `cadence-java-client` (hopefully that's you!)

**Note:** All contributors also need to fill out the [Uber Contributor License Agreement](http://t.uber.com/cla) before we can merge in any of your changes

## Development Environment

* Java 11 (currently, we use Java 11 to compile Java 8 code).
* Thrift 0.9.3 (use [homebrew](https://formulae.brew.sh/formula/thrift@0.9) or [distribution](https://downloads.apache.org/thrift/0.9.3/)). Alternatively you can install with `scripts/install-thrift-locally-osx.sh`.
* Gradle build tool [6.x](https://github.com/uber/cadence-java-client/blob/master/gradle/wrapper/gradle-wrapper.properties)
* Docker

:warning: Note 1: You must install the 0.9.x version of Thrift. Otherwise compiling would fail at error `error: package org.apache.thrift.annotation does not exist`

:warning: Note 2: It's currently compatible with Java 8 compiler but no guarantee in the future. 

## IntelliJ IDE integration (Optional)

* Make sure you set the gradle path with the right version ([currently 6.x](https://github.com/uber/cadence-java-client/blob/master/gradle/wrapper/gradle-wrapper.properties))

![IntelliJ](https://user-images.githubusercontent.com/4523955/135696878-81c1e62e-eb04-45e6-9bcb-785ac38b6607.png)

* Then all the below `gradlew` command can be replaced with the Gradle plugin operation 
![Gradle](https://user-images.githubusercontent.com/4523955/135696922-d43bc36d-18a4-4b7b-adee-0fe8300bf855.png)

## Licence headers

This project is Open Source Software, and requires a header at the beginning of
all source files. To verify that all files contain the header execute:

```lang=bash
./gradlew license
```

To generate licence headers execute

```lang=bash
./gradlew licenseFormat
```

## Commit Messages

Overcommit adds some requirements to your commit messages. At Uber, we follow the
[Chris Beams](http://chris.beams.io/posts/git-commit/) guide to writing git
commit messages. Read it, follow it, learn it, love it.


##  Build & Publish & Test locally
Build with:

```bash
./gradlew build
```

To test locally, you can publish to [MavenLocal](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:case-for-maven-local)

1. Change `build.gradle`:
Comment out the first section in `publications` ( line 160 to line 191 in [this commit](https://github.com/uber/cadence-java-client/blob/c9ec6786aa9f866b0310292ea3ee5df63adc8799/build.gradle#L160))

2. Change the [version](https://github.com/uber/cadence-java-client/blob/c9ec6786aa9f866b0310292ea3ee5df63adc8799/build.gradle#L43) to add a `local` suffix. E.g.
```
version = '3.3.0'
```` 
to 
```
version = '3.3.0-local'
``` 
Then run the command
```bash
./gradlew publishToMavenLocal
```
Now you have the local cadence-java-client in your machine using veriosn `3.3.0-local`

3. To test with Cadence Java Samples, [change](https://github.com/uber/cadence-java-samples/blob/master/build.gradle#L32) `mavenCentral()` to `mavenLocal()`
and also change the [version](https://github.com/uber/cadence-java-samples/blob/a79d8d6e5860cf9986bf549fc1f96badecb09f8f/build.gradle#L38) with your suffix. 

Then `./gradlew build` and refer to the sample repo for how to run the code(it needs to run with a [Cadence server](https://github.com/uber/cadence)). 

:warning: If you run into problem with `version.properties` [creation task](https://github.com/uber/cadence-java-client/blob/c9ec6786aa9f866b0310292ea3ee5df63adc8799/build.gradle#L109), you can comment the task out. It's okay for local testing.  
The property file is being used by [Version class](https://github.com/uber/cadence-java-client/blob/master/src/main/java/com/uber/cadence/internal/Version.java#L39)to report the library version for logging/metrics. 

## Unit & Integration Test

Then run all the tests with:

```bash
./gradlew test
```

The test by default will run with TestEnvironment without Cadence service. If you want to run with Cadence serivce:
```bash
USE_DOCKER_SERVICE=true ./gradlew test
```
And sometimes it's important to test the non-sticky mode 
```bash
STICKY_OFF=true USE_DOCKER_SERVICE=true ./gradlew test
```

Also, if there is any Github Actions test failure that you cannot reproduce locally, 
follow [github action docker-compose](./docker/github_actions/README.md) instructions to run the tests.
