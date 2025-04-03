# Java framework for Cadence [![Build Status](https://badge.buildkite.com/0c96b8b74c0921208e898c10a602e2fe9ecb7641c2befee0e7.svg?theme=github&branch=master)](https://buildkite.com/uberopensource/cadence-java-client) [![Javadocs](https://www.javadoc.io/badge/com.uber.cadence/cadence-client.svg)](https://www.javadoc.io/doc/com.uber.cadence/cadence-client) [![codecov](https://codecov.io/gh/cadence-workflow/cadence-java-client/graph/badge.svg?token=eVBGf4EmXr)](https://codecov.io/gh/cadence-workflow/cadence-java-client)


[Cadence](https://github.com/cadence-workflow/cadence) is a distributed, scalable, durable, and highly available orchestration engine we developed at Uber Engineering to execute asynchronous long-running business logic in a scalable and resilient way.

`cadence-client` is the framework for authoring workflows and activities in Java.

If you are authoring in Go, see [Go Cadence Client](https://github.com/cadence-workflow/cadence-go-client).

## Samples

For samples, see [Samples for the Java Cadence client](https://github.com/cadence-workflow/cadence-java-samples).

## Run Cadence Server

Run Cadence Server using Docker Compose:

    curl -O https://raw.githubusercontent.com/cadence-workflow/cadence/master/docker/docker-compose.yml
    docker-compose up

If this does not work, see [instructions](https://github.com/cadence-workflow/cadence/blob/master/README.md) for running the Cadence Server

## Get CLI

[CLI is available as an executable or as a docker image](https://github.com/cadence-workflow/cadence/blob/master/tools/cli/README.md)

## Build a configuration

Add *cadence-client* as a dependency to your *pom.xml*:

    <dependency>
      <groupId>com.uber.cadence</groupId>
      <artifactId>cadence-client</artifactId>
      <version>V.V.V</version>
    </dependency>
    
or to *build.gradle*:

    compile group: 'com.uber.cadence', name: 'cadence-client', version: 'V.V.V'

## Documentation

The documentation on how to use the Cadence Java client is [here](https://cadenceworkflow.io/docs/java-client).

Javadocs for the client API are located [here](https://www.javadoc.io/doc/com.uber.cadence/cadence-client).

## Contributing
We'd love your help in making the Cadence Java client great. Please review our [contribution guidelines](CONTRIBUTING.md).

## License
Apache License, please see [LICENSE](LICENSE) for details.
