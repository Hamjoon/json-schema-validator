{
    "test_cases": [
        {
            "group": "Custom Configurations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"https://example.org/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"id\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); setFailFast(true); } }"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "When a non‑default configuration enabling type‑loose validation and fail‑fast mode is supplied, the factory must still return a valid JsonSchema instance."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\",\\\"maxLength\\\":5}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig(){ { setReadOnly(true); } }"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "A configuration that marks the schema as read‑only should not affect schema creation; a JsonSchema object must be returned."
                }
            ]
        },
        {
            "group": "Various URI Schemes and Components",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"ftp://example.com/resources/schema.json\")"
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
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "The factory must accept URIs with the FTP scheme and still produce a JsonSchema."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"jar:file:/opt/app.jar!/schema.json\")"
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
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "A JAR URI should be handled like any other location; the method should return a JsonSchema instance."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://user:pass@example.com:8080/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"null\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "URI containing user‑info and explicit port must be accepted without error."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/%7Euser/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"string\\\"}}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "Encoded characters in the path component must be correctly normalized; the schema should be created successfully."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"schema-relative.json\")"
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
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "A relative URI (no scheme) is allowed; the factory should still produce a JsonSchema instance."
                }
            ]
        },
        {
            "group": "Schema Node Edge Cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/empty.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "An empty JSON object is a valid (though minimal) schema; the method must return a JsonSchema instance."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/primitive.json\")"
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
                    "expected": "IllegalArgumentException",
                    "description": "A schema node that is not a JSON object (e.g., a plain string) is illegal and should cause an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/ref.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"$ref\\\":\\\"#/definitions/foo\\\",\\\"definitions\\\":{\\\"foo\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "A schema containing a $ref that resolves within the same document must be accepted and result in a JsonSchema instance."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/invalid-ref.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"$ref\\\":\\\"http://nonexistent/schema.json\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null)",
                    "description": "Even when a $ref points to a non‑existent external schema, getSchema should still create a JsonSchema object; resolution errors are deferred to validation time."
                }
            ]
        },
        {
            "group": "Meta‑schema Detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/draft04.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-04/schema#\\\",\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null) using Draft‑04 meta‑schema",
                    "description": "When the schema explicitly declares the Draft‑04 meta‑schema, the factory must load that meta‑schema and create the JsonSchema accordingly."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/draft07.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"string\\\"}}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null) using Draft‑07 meta‑schema",
                    "description": "The factory must recognise the Draft‑07 identifier and load the corresponding meta‑schema."
                }
            ]
        },
        {
            "group": "Unsupported or Malformed URI Handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"mailto:user@example.com\")"
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
                    "expected": "IllegalArgumentException",
                    "description": "A URI with an unsupported scheme (mailto) should cause the factory to reject the location and throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/space in path.json\")"
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
                    "expected": "IllegalArgumentException",
                    "description": "A URI containing unescaped spaces is considered malformed; the factory should detect this and throw IllegalArgumentException."
                }
            ]
        }
    ]
}