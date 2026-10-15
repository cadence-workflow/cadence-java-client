//go:build tools

// This file imports golang.org/x/net/idna so that go mod tidy keeps the
// golang.org/x/net pin in go.mod. Without an import, tidy removes indirect
// deps that the main module does not actually use — which would silently
// revert x/net to the vulnerable version pulled in by thriftrw.
package main

import _ "golang.org/x/net/idna"
