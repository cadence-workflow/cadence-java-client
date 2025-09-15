package main

import "fmt"

func main() {
    if err := NewGenerator().Generate("../../src/main/idls/thrift/shared.thrift", "../../src/gen/java", "com.uber.cadence"); err != nil {
        panic(fmt.Sprintf("failed to generate: %v", err))
    }
    if err := NewGenerator().Generate("../../src/main/idls/thrift/shadower.thrift", "../../src/gen/java", "com.uber.cadence.shadower"); err != nil {
        panic(fmt.Sprintf("failed to generate: %v", err))
    }
}
