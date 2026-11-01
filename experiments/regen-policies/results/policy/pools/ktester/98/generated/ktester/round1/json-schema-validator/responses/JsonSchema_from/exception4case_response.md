{
    "test_cases": [
        {
            "group": "JsonSchema.from - schemaNode as numeric node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"http://example.com/schema\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.numberNode(42)"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "A numeric JsonNode is not a valid JSON Schema representation; the factory method should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchema.from - schemaNode as string node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"http://example.com/schema\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.textNode(\"not a schema\")"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "A text JsonNode cannot be interpreted as a JSON Schema; the constructor should raise IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchema.from - schemaNode as missing node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"http://example.com/schema\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.missingNode()"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "A missing node indicates that no schema definition was supplied; the factory should fail with IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchema.from - invalid SchemaLocation URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"ht!tp://[invalid-uri]\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "SchemaLocation constructor validates the URI; providing a malformed URI should cause IllegalArgumentException during the call to JsonSchema.from."
                }
            ]
        }
    ]
}