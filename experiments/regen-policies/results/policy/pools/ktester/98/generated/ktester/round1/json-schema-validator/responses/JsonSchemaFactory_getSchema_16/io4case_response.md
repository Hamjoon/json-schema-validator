{
    "testCases": [
        {
            "group": "Relative URI handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"/relative/path/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaLocation string equals \"/relative/path/schema.json\" and its schemaNode equals the supplied JsonNode",
                    "description": "Pass a relative URI (no scheme, host, or authority) to verify that the factory does not reject it and stores the exact string in the created SchemaLocation."
                }
            ]
        },
        {
            "group": "Empty URI string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaLocation string is empty (\"\") and its schemaNode equals the supplied JsonNode",
                    "description": "An empty URI string should be accepted; the resulting JsonSchema should carry an empty location without throwing an exception."
                }
            ]
        },
        {
            "group": "Special characters in URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schemas/my%20schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"number\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaLocation string includes the encoded space \"%20\" exactly as supplied",
                    "description": "Ensures that URI encoding is preserved when the factory creates the SchemaLocation."
                }
            ]
        },
        {
            "group": "Non‑object JsonNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"\\\"just a string\\\"\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaNode is a TextNode with value \"just a string\"",
                    "description": "The factory should accept any JsonNode, not only object nodes; verify that a primitive JSON value is stored unchanged."
                }
            ]
        },
        {
            "group": "NullNode JsonNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.nullNode()"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaNode is a NullNode (represents JSON literal null)",
                    "description": "Distinguish between a null reference (already covered) and a JsonNode that explicitly represents JSON null."
                }
            ]
        },
        {
            "group": "Combined config flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\", \\\"properties\\\":{}}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig(){ { setFailFast(true); setTypeLoose(true); setReadOnly(true); } }"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its internal ValidationContext reflects failFast, typeLoose and readOnly all set to true",
                    "description": "Pass a configuration object that enables several flags simultaneously and verify that all flags are propagated to the created JsonSchema."
                }
            ]
        },
        {
            "group": "URI with user info and port",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://user:pass@example.com:8080/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaLocation string equals \"http://user:pass@example.com:8080/schema.json\"",
                    "description": "Validate that authority components (user info, port) are retained unchanged in the SchemaLocation."
                }
            ]
        },
        {
            "group": "URI with fragment and query together",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json?ver=2#section\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; its schemaLocation string includes both the query \"?ver=2\" and the fragment \"#section\" exactly as supplied",
                    "description": "Ensures that when both a query string and a fragment are present, the full URI is preserved without loss or reordering."
                }
            ]
        }
    ]
}