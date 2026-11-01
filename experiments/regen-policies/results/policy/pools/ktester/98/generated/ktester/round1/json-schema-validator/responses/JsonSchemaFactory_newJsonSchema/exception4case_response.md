{
    "test_cases": [
        {
            "group": "Primitive schemaNode root – not an object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/primitive-root.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new ObjectMapper().readTree(\"\\\"just a string\\\"\")"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) thrown because the schema root must be a JSON object, not a primitive value.",
                    "description": "Ensures that a schema supplied as a JSON primitive (string, number, boolean, or null) is rejected. The factory should detect that the root node is not an object and raise an appropriate exception before attempting to create a JsonSchema."
                }
            ]
        },
        {
            "group": "Unsupported URI scheme in schemaUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new ObjectMapper().readTree('{\"type\":\"object\"}')"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown from getSchemaLocation because the URI scheme (ftp) is not supported by the factory.",
                    "description": "Verifies that the factory validates the scheme of the supplied SchemaLocation. Only supported schemes (e.g., http, https, classpath, file) should be accepted; an unsupported scheme must cause an early failure."
                }
            ]
        },
        {
            "group": "Config with null path type causing NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/null-pathtype.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new ObjectMapper().readTree('{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}')"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig().setPathType(null)"
                        }
                    ],
                    "expected": "NullPointerException thrown when constructing JsonNodePath because the path type resolved from the configuration is null.",
                    "description": "Tests the behaviour when a custom SchemaValidatorsConfig explicitly sets a null path type. The factory later calls validationContext.getConfig().getPathType() to build a JsonNodePath; a null value should result in a NullPointerException, confirming that the method does not silently accept an invalid configuration."
                }
            ]
        }
    ]
}