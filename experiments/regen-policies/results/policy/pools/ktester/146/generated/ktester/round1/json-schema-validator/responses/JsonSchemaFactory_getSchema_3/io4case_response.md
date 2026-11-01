{
    "testCases": [
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - boolean schema literals",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "true"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance representing a schema that always validates (boolean true schema).",
                    "description": "Pass a JsonNode that is the JSON literal `true`. According to the JSON Schema spec this represents a schema that always passes validation. The factory should return a valid JsonSchema object without throwing."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "false"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance representing a schema that never validates (boolean false schema).",
                    "description": "Pass a JsonNode that is the JSON literal `false`. This represents a schema that always fails validation. The method must still create a JsonSchema instance."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - unsupported primitive root",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "123"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Pass a JsonNode that is a numeric literal. JSON Schema documents must be objects (or boolean literals). The factory should reject this with a JsonSchemaException (or IllegalArgumentException) indicating an invalid schema type."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "\"string\""
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Pass a JsonNode that is a string literal. The method should throw a JsonSchemaException because a schema cannot be a plain string."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - unknown $schema version",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Provide a schema node that declares an unsupported meta‑schema version (draft‑04). The factory should detect the unknown version and throw an IllegalArgumentException (or a JsonSchemaException) rather than silently defaulting."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - schema with $id and internal $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$id\": \"#root\", \"definitions\": { \"name\": { \"type\": \"string\" } }, \"$ref\": \"#/definitions/name\" }"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance whose root resolves to a string type via internal $ref.",
                    "description": "Pass a schema that defines an internal definition and references it with $ref. Even though resolution requires a base URI, the factory should still return a JsonSchema object (resolution may be deferred)."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - explicit type null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"null\" }"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance with type constraint \"null\".",
                    "description": "Pass a schema that explicitly allows only null values. The method should create a JsonSchema reflecting this constraint."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - empty enum array",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"enum\": [] }"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance representing an always‑invalid schema (empty enum).",
                    "description": "Provide a schema with an empty `enum`. According to the spec this schema can never be satisfied; the factory should still return a JsonSchema object."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - unknown keyword tolerance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"object\", \"unknownKeyword\": 42 }"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance that ignores the unknown keyword.",
                    "description": "Pass a schema containing a non‑standard keyword. The factory should ignore unknown keywords (per spec) and still produce a valid JsonSchema instance."
                }
            ]
        }
    ]
}