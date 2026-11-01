{
    "test_cases": [
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid $id format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"$id\": \"http://example .com\", \"type\": \"object\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Provide a schema with an $id containing an invalid URI (space in host). The factory should detect the malformed $id and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid $ref format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"$ref\": \"http://example .com#/def\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Provide a schema with a $ref containing an invalid URI (space in host). The factory should throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid type keyword value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\": \"invalidtype\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema with a 'type' keyword having an unsupported type string. The factory should reject it with JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\": [\"string\", \"invalidtype\"]}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'type' is an array containing an invalid type. The factory should throw JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid required keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"required\": \"name\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'required' is a string instead of an array. The factory should throw JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"required\": [123, true]}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'required' array contains non‑string items. The factory should throw JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid properties keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"properties\": \"notAnObject\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'properties' is not an object. The factory should reject it with JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid definitions keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"definitions\": []}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'definitions' is an array instead of an object. Expect JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - malformed $schema URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"$schema\": \"http://example .com/draft-07/schema#\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Provide a $schema value that is not a valid URI (contains space). Expect IllegalArgumentException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid numeric constraints",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"minimum\": \"abc\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'minimum' is a non‑numeric value. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"multipleOf\": 0}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'multipleOf' is zero (must be > 0). Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"maxLength\": -5}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'maxLength' is negative. Expect JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid additionalProperties",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"additionalProperties\": 123}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'additionalProperties' is a number instead of a boolean or schema object. Expect JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid patternProperties",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"patternProperties\": \"notAnObject\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'patternProperties' is not an object. Expect JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid dependencies",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"dependencies\": []}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'dependencies' is an array instead of an object. Expect JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid combinator keywords",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"allOf\": {\"type\": \"string\"}}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'allOf' is an object instead of an array. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"anyOf\": \"string\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'anyOf' is a string instead of an array. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"oneOf\": 123}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'oneOf' is a number instead of an array. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"not\": []}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'not' is an array instead of an object. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"if\": []}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'if' is an array instead of an object. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"then\": \"string\"}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'then' is a string instead of an object. Expect JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{\"else\": 123}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "Provide a schema where 'else' is a number instead of an object. Expect JsonSchemaException."
                }
            ]
        }
    ]
}