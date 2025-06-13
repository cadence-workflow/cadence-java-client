package main

import (
	"fmt"
	"log"
	"os"
	"strings"
	"sync"
	"text/template"

	"go.uber.org/thriftrw/ast"
	"go.uber.org/thriftrw/idl"
)

const (
	baseExceptionClassName = "BaseError"
)

type Field struct {
	Name string
	Type string
}

type TemplateEntity struct {
	PackageName string
	ClassName string
	Fields    []Field
}

type TemplateEnum struct {
	PackageName string
	ClassName string
	Fields    []string
}

type TemplateException struct {
	PackageName string
	ClassName string
	Fields    []Field
	BaseExceptionClassName string
}

type TemplateBaseException struct {
	PackageName string
	ClassName string
}

type Generator struct {
	tmplStruct *template.Template
	tmplEnum *template.Template
	tmplException *template.Template
	tmplBaseException *template.Template
	log *log.Logger
}

func NewGenerator() *Generator {
	return &Generator{
		tmplStruct: template.Must(template.ParseFiles("./template/java_struct.tmpl")),
		tmplEnum: template.Must(template.ParseFiles("./template/java_enum.tmpl")),
		tmplException: template.Must(template.ParseFiles("./template/java_exception.tmpl")),
		tmplBaseException: template.Must(template.ParseFiles("./template/java_base_exception.tmpl")),
		log: log.New(os.Stdout, "", log.LstdFlags),
	}
}

func (g *Generator) Generate(input string, outputDir string, packageNameOverride string) error {
	config := &idl.Config{}

	content, err := os.ReadFile(input)
	if err != nil {
		return fmt.Errorf("failed to read file: %w", err)
	}

	program, err := config.Parse(content)
	if err != nil {
		return fmt.Errorf("failed to parse file: %w", err)
	}

	packageName := packageNameOverride
	if packageName == "" {
		packageName, err = getPackageName(program)
		if err != nil {
			return fmt.Errorf("failed to get package name: %w", err)
		}
	}
	outputDir = fmt.Sprintf("%s/%s", outputDir, strings.ReplaceAll(packageName, ".", "/"))

	err = os.MkdirAll(outputDir, 0755)
	if err != nil {
		return fmt.Errorf("failed to create output directory: %w", err)
	}

	ast.Walk(ast.VisitorFunc(func(w ast.Walker, n ast.Node) {
		var err error
		switch v:=n.(type) {
		case *ast.Struct:
			switch v.Type {
			case ast.ExceptionType:
				err = g.generateException(v, outputDir, packageName)
			case ast.StructType:
				err = g.generateStruct(v, outputDir, packageName)
			}
		case *ast.Enum:
			err = g.generateEnum(v, outputDir, packageName)
		}
		if err != nil {
			g.log.Fatalf("failed to generate: %v", err)
		}
	}), program)
	return nil
}

func (g *Generator) generateStruct(v *ast.Struct, outputDir string, packageName string) error {
	fields := make([]Field, 0)
	for _, field := range v.Fields {
		typeStr, err := typeMapper(field.Type, true)
		if err != nil {
			return fmt.Errorf("failed to map field type: %w", err)
		}

		fields = append(fields, Field{
			Name: field.Name,
			Type: typeStr,
		})
	}

	data := TemplateEntity{
		PackageName: packageName,
		ClassName: v.Name,
		Fields:    fields,
	}

	outputFile := fmt.Sprintf("%s/%s.java", outputDir, v.Name)
	f, err := os.Create(outputFile)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer f.Close()

	if err := g.tmplStruct.Execute(f, data); err != nil {

		return fmt.Errorf("failed to execute template: %w", err)
	}

	return nil
}

func (g *Generator) generateException(v *ast.Struct, outputDir string, packageName string) error {
	var once sync.Once
	once.Do(func(){
		err := g.generateBaseException(baseExceptionClassName, outputDir, packageName)
		if err != nil {
			g.log.Fatalf("failed to generate base exception: %v", err)
		}
	})

	fields := make([]Field, 0)
	for _, field := range v.Fields {
		if field.Name == "message" { // skip on message field, it is already in the base exception
			continue
		}
		typeStr, err := typeMapper(field.Type, true)
		if err != nil {
			return fmt.Errorf("failed to map field type: %w", err)
		}
		fields = append(fields, Field{
			Name: field.Name,
			Type: typeStr,
		})
	}

	data := TemplateException{
		PackageName: packageName,
		ClassName: v.Name,
		Fields: fields,
		BaseExceptionClassName: baseExceptionClassName,
	}

	outputFile := fmt.Sprintf("%s/%s.java", outputDir, v.Name)
	f, err := os.Create(outputFile)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer f.Close()

	if err := g.tmplException.Execute(f, data); err != nil {
		return fmt.Errorf("failed to execute template: %w", err)
	}
	return nil
}

func (g *Generator) generateBaseException(className string, outputDir string, packageName string) error {
	data := TemplateBaseException{
		PackageName: packageName,
		ClassName: className,
	}

	outputFile := fmt.Sprintf("%s/%s.java", outputDir, className)
	f, err := os.Create(outputFile)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer f.Close()

	if err := g.tmplBaseException.Execute(f, data); err != nil {
		return fmt.Errorf("failed to execute template: %w", err)
	}
	return nil
}

func (g *Generator) generateEnum(v *ast.Enum, outputDir string, packageName string) error {
	data := TemplateEnum{
		PackageName: packageName,
		ClassName:   v.Name,
	}
	for _, item := range v.Items {
		data.Fields = append(data.Fields, item.Name)
	}

	outputFile := fmt.Sprintf("%s/%s.java", outputDir, v.Name)
	f, err := os.Create(outputFile)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer f.Close()

	if err := g.tmplEnum.Execute(f, data); err != nil {
		return fmt.Errorf("failed to execute template: %w", err)
	}
	return nil
}

func getPackageName(program *ast.Program) (string, error) {
	for _, header := range program.Headers {
		if header, ok := header.(*ast.Namespace); ok && header.Scope == "java" {
			return header.Name, nil
		}
	}
	return "", fmt.Errorf("cannot find package name in the thrift file")
}

func typeMapper(t ast.Type, usePrimitive bool) (string, error) {
	switch tt :=t.(type) {
	case ast.BaseType:
		return baseTypeMapper(tt, usePrimitive)
	case ast.MapType:
		keyType, err := typeMapper(tt.KeyType, false)
		if err != nil {
			return "", fmt.Errorf("failed to map key type: %w", err)
		}
		valueType, err := typeMapper(tt.ValueType, false)
		if err != nil {
			return "", fmt.Errorf("failed to map value type: %w", err)
		}
		return "Map<" + keyType + ", " + valueType + ">", nil
	case ast.ListType:
		valueType, err := typeMapper(tt.ValueType, false)
		if err != nil {
			return "", fmt.Errorf("failed to map value type: %w", err)
		}
		return "List<" + valueType + ">", nil
	case ast.SetType:
		valueType, err := typeMapper(tt.ValueType, false)
		if err != nil {
			return "", fmt.Errorf("failed to map value type: %w", err)
		}
		return "Set<" + valueType + ">", nil
	case ast.TypeReference:
		return tt.Name, nil
	default:
		return "", fmt.Errorf("do not support type: %v", tt)
	}
}

func baseTypeMapper(t ast.BaseType, usePrimitive bool) (string, error) {
	switch t.ID {
	case ast.BoolTypeID:
		if usePrimitive {
			return "boolean", nil
		}
		return "Boolean", nil
	case ast.I8TypeID, ast.I16TypeID, ast.I32TypeID:
		if usePrimitive {
			return "int", nil
		}
		return "Integer", nil
	case ast.I64TypeID:
		if usePrimitive {
			return "long", nil
		}
		return "Long", nil
	case ast.DoubleTypeID:
		if usePrimitive {
			return "double", nil
		}
		return "Double", nil
	case ast.StringTypeID:
		return "String", nil
	case ast.BinaryTypeID:
		return "byte[]", nil
	default:
		return "", fmt.Errorf("unknown base type: %v", t.ID)
	}
}
