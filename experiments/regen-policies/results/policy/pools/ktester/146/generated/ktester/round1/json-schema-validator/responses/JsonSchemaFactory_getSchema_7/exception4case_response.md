{
    "new_cases": [
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) – schema structure validation errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "validJsonStreamContainingNonObjectRoot (e.g. the literal \"true\" or \"[]\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws JsonSchemaException (or IllegalArgumentException) because the parsed JSON is not an object, which is required for a JSON Schema root",
                    "description": "The method parses the stream successfully but then validates that the schema node is an object; a non‑object root should cause a schema‑validation exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "validJsonStreamWithUnknownMetaSchema (e.g. {\"$schema\":\"http://example.com/unknown\", \"type\":\"object\"})"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws JsonSchemaException (or IllegalArgumentException) indicating that the meta‑schema identified by the $schema keyword cannot be located or is unsupported",
                    "description": "When the schema declares an unknown or unsupported meta‑schema URI, the factory should fail during meta‑schema loading."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "validJsonStreamWithUnresolvableRef (e.g. {\"$ref\":\"http://nonexistent.example/schema.json\"})"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws JsonSchemaException (or JsonSchemaException wrapping an IOException) because the external reference cannot be resolved by the SchemaLoader",
                    "description": "A schema that contains a $ref to a URI that the factory cannot load should cause a resolution failure during schema loading."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) – stream state errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "closedInputStream (a ByteArrayInputStream that has been closed before the call)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws IOException (or JsonSchemaException wrapping IOException) because reading from a closed stream fails",
                    "description": "Ensures that the factory propagates an I/O error when the supplied InputStream is already closed."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "truncatedJsonStream (contains only the beginning of a valid JSON object, e.g. \"{\\\"type\\\":\\\"object\\\"\" without the closing brace)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (wrapped in JsonSchemaException) due to unexpected end‑of‑input while parsing",
                    "description": "A stream that ends prematurely should trigger a parsing exception, confirming that the method does not silently accept incomplete JSON."
                }
            ]
        }
    ]
}