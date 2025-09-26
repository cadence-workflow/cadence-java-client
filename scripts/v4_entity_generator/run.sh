#! /bin/bash

# This script is used to generate the java entities from the thrift files. We will eventually remove this script and maintain entities without using thrift.
ROOT_DIR=$(git rev-parse --show-toplevel)
cd $ROOT_DIR/scripts/v4_entity_generator

go run main.go generator.go

cd $ROOT_DIR
./gradlew googleJavaFormat
