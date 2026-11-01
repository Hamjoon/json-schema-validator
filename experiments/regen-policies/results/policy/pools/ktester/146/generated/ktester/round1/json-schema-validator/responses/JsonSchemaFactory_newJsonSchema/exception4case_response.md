{
    "testCases": [
        {
            "group": "Invalid SchemaLocation format – should reject malformed URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "invalid://@@@"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the supplied SchemaLocation cannot be parsed/normalized, getSchemaLocation is expected to throw IllegalArgumentException, which propagates out of newJsonSchema."
                }
            ]
        },
        {
            "group": "Config with null PathType – JsonNodePath construction fails",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocation"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithNullPathType"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the SchemaValidatorsConfig supplied contains a null path type, validationContext.getConfig().getPathType() returns null and the JsonNodePath constructor throws NullPointerException."
                }
            ]
        },
        {
            "group": "Non‑object JsonNode – schema node must be an object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocation"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "jsonPrimitiveNode"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "doCreate expects the root schema node to be a JSON object; passing a primitive (e.g., a text node) should cause it to throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unsupported $schema identifier – version flag not recognised",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocation"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "schemaNodeWithUnsupportedMetaSchema"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the schema JSON contains a $schema value that does not map to any known JsonMetaSchema, doCreate should raise IllegalArgumentException indicating an unsupported schema version."
                }
            ]
        },
        {
            "group": "Circular schema reference – detection triggers IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockCircularSchemaLocation"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "circularSchemaNode"
                        },
                        {
                            "parameter": "config",
                            "value": "defaultConfig"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If the supplied schema (or its $ref chain) creates a circular reference, doCreate is expected to detect it and throw IllegalStateException."
                }
            ]
        }
    ]
}