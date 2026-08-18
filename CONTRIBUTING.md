# Developing cadence-java-client

This doc is intended for contributors to `cadence-java-client` (hopefully that's you!)

📚 **New to contributing to Cadence?** Check out our [Contributing Guide](https://cadenceworkflow.io/community/how-to-contribute/getting-started) for an overview of the contribution process across all Cadence repositories. This document contains cadence-java-client specific setup and development instructions.

Join our community on the CNCF Slack workspace at [cloud-native.slack.com](https://communityinviter.com/apps/cloud-native/cncf) in the **#cadence-users** channel to reach out and discuss issues with the team.

## Development Environment

* Java 11 (currently, we use Java 11 to compile Java 8 code).
* Gradle build tool [6.x](https://github.com/cadence-workflow/cadence-java-client/blob/master/gradle/wrapper/gradle-wrapper.properties)
* Docker

:warning: Note 1: It's currently compatible with Java 8 compiler but no guarantee in the future.

## IntelliJ IDE integration (Optional)

* Make sure you set the gradle path with the right version ([currently 6.x](https://github.com/cadence-workflow/cadence-java-client/blob/master/gradle/wrapper/gradle-wrapper.properties))

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


## Versioning

The project version is derived automatically from git tags via `git describe --tags` (leading `v` is stripped). You do **not** edit a hard-coded version in `build.gradle`.

Examples:

| Git state | Published version |
| --- | --- |
| Exact tag `v3.13.3` | `3.13.3` |
| 2 commits after `v3.13.3` at `36ed6879` | `3.13.3-2-g36ed6879` |

Check the current version (printed during Gradle configuration):

```bash
./gradlew printVersion
# or: git describe --tags
```

Maven coordinates:

```text
com.uber.cadence:cadence-client:<version>
```

## Build

```bash
./gradlew build
```

## Test locally against Maven Local

Publish the current git-describe version to [Maven Local](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:case-for-maven-local):

```bash
./gradlew publishToMavenLocal
```

Artifacts land under:

```text
~/.m2/repository/com/uber/cadence/cadence-client/<version>/
  cadence-client-<version>.jar
  cadence-client-<version>-sources.jar
  cadence-client-<version>-javadoc.jar
```

To consume it from another project (for example [cadence-java-samples](https://github.com/cadence-workflow/cadence-java-samples)):

1. Prefer `mavenLocal()` over `mavenCentral()` (or list `mavenLocal()` first).
2. Set the dependency version to the printed git-describe version, e.g. `3.13.3-2-g36ed6879`.
3. Build/run that project. Samples need a running [Cadence server](https://github.com/cadence-workflow/cadence).

:warning: `createProperties` writes `version.properties` used by [`Version`](src/main/java/com/uber/cadence/internal/Version.java) for logging/metrics. If that task fails during local testing, you can temporarily disable it; for real releases leave it enabled.

## Unit & Integration Test

Run all tests:

```bash
./gradlew test
```

By default tests use the in-process TestEnvironment (no Cadence service). To run against a Cadence service in Docker:

```bash
USE_DOCKER_SERVICE=true ./gradlew test
```

Non-sticky mode:

```bash
STICKY_OFF=true USE_DOCKER_SERVICE=true ./gradlew test
```

If a GitHub Actions failure is hard to reproduce locally, follow [github action docker-compose](./docker/github_actions/README.md).

## Release to Maven Central

Releases are published by the [Release to Maven Central](.github/workflows/release.yml) GitHub Action. Version still comes from git tags.

1. Ensure release notes / `CHANGELOG.md` are updated and the release commit is on the intended branch.
2. Create and push an annotated (or lightweight) version tag matching `v*`, for example:

```bash
git tag v3.13.4
git push origin v3.13.4
```

3. Pushing `v*` triggers the workflow, which *already* runs:

```bash
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

   using OSSRH and GPG signing secrets configured in the repo.

4. Alternatively, run the workflow manually via **Actions → Release to Maven Central → Run workflow** (`workflow_dispatch`). The published version is still whatever `git describe --tags` resolves to for the checked-out ref—so for a clean release, run it from (or after checking out) the release tag.

5. After Sonatype processing completes, verify the artifact on Maven Central:

```text
com.uber.cadence:cadence-client:<version>
```

Maintainers only: local/manual publish to Sonatype requires `ossrhUsername` / `ossrhPassword` and signing properties (`signing.keyId`, `signing.key` / local keyring, `signing.password`) as used by `build.gradle`. Prefer the GitHub Action for official releases.
