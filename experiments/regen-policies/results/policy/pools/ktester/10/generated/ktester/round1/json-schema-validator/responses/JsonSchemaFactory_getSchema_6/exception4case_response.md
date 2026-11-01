{
    "testCases": [
        {
            "group": "Invalid Schema Content - Non‑JSON/YAML File",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/not-a-schema.txt\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) indicating that the resource could not be parsed as JSON/YAML",
                    "description": "The URI points to a plain text file that is not valid JSON or YAML. The factory should attempt to parse it and fail, throwing a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Invalid JSON Syntax",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/invalid-json-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or JsonProcessingException) caused by malformed JSON structure",
                    "description": "The referenced file contains syntactically invalid JSON (e.g., missing commas, mismatched braces). Parsing must fail and the factory should surface the parsing exception."
                }
            ]
        },
        {
            "group": "Unsupported $schema Declaration",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/unknown-meta-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the $schema value is not recognized",
                    "description": "The schema declares a $schema identifier that does not correspond to any known Draft version. The factory should reject the schema with a clear error."
                }
            ]
        },
        {
            "group": "Circular $ref Resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/circular-ref-a.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) reporting a circular reference detection",
                    "description": "Schema A $ref's Schema B, which in turn $ref's back to Schema A, creating a loop. The loader must detect the cycle and abort with an appropriate exception."
                }
            ]
        },
        {
            "group": "Invalid $ref Target Not Found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/ref-to-missing.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) indicating that the $ref target could not be resolved",
                    "description": "The schema contains a $ref that points to a non‑existent file or fragment. Resolution should fail and the factory must raise an exception."
                }
            ]
        },
        {
            "group": "Schema with Empty Content",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/empty-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) because the input stream contains no data to parse",
                    "description": "An empty file is supplied as the schema source. The parser should detect the lack of content and throw an exception."
                }
            ]
        },
        {
            "group": "Unsupported Media Type - .xml Extension",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/schema.xml\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the file type is not supported for schema loading",
                    "description": "The URI points to an XML file. Since the factory only handles JSON/YAML, it must reject the resource with a clear error."
                }
            ]
        }
    ]
}