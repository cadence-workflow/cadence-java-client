package main

import "fmt"

func main() {
    if err := NewGenerator().Generate("../../src/main/idls/thrift/shared.thrift", "../../src/gen/java", "com.uber.cadence.entities"); err != nil {
		panic(fmt.Sprintf("failed to generate: %v", err))
	}
}
