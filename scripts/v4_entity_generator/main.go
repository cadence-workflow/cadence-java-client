package main

import "fmt"

/*
This script is used to generate the java entities from the thrift files. We will eventually remove this script and maintain entities without using thrift.
*/

func main() {
    if err := NewGenerator().Generate("../../src/main/idls/thrift/shared.thrift", "../../src/gen/java", "com.uber.cadence"); err != nil {
        panic(fmt.Sprintf("failed to generate: %v", err))
    }
    if err := NewGenerator().Generate("../../src/main/idls/thrift/shadower.thrift", "../../src/gen/java", "com.uber.cadence.shadower"); err != nil {
        panic(fmt.Sprintf("failed to generate: %v", err))
    }
}
