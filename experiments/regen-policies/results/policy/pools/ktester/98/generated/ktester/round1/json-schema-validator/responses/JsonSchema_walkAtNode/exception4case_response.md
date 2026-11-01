{
    "testCases": [
        {
            "group": "Null parameter handling – executionContext is null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing { \"name\": \"John\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "walkAtNode should immediately fail with NPE when the required ExecutionContext argument is null."
                }
            ]
        },
        {
            "group": "Null parameter handling – instanceLocation is null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mocked ExecutionContext with default settings"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing { \"age\": 30 }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "null"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "The method relies on a non‑null JsonNodePath to build error locations; passing null should cause an NPE."
                }
            ]
        },
        {
            "group": "Invalid JsonNodePath – malformed pointer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mocked ExecutionContext with default settings"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing { \"name\": \"John\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/invalid~path\")"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (malformed JSON Pointer)",
                    "description": "JsonNodePath internally validates JSON‑Pointer syntax. Supplying a pointer with an illegal '~' escape should trigger an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unresolvable $ref fragment – IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mocked ExecutionContext where getRefSchemaNode(\"#/nonexistent\") returns null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing { \"$ref\": \"#/nonexistent\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (cannot resolve $ref fragment)",
                    "description": "When a $ref points to a fragment that does not exist in the schema, walkAtNode should raise an IllegalArgumentException rather than silently ignoring it."
                }
            ]
        },
        {
            "group": "ExecutionContext method throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mocked ExecutionContext whose getValidators() throws IllegalStateException(\"custom failure\")"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing { \"name\": \"John\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalStateException is propagated",
                    "description": "If the ExecutionContext encounters an unexpected error while providing validators, walkAtNode must not swallow the exception; it should propagate the original RuntimeException."
                }
            ]
        },
        {
            "group": "Schema contains unsupported format – DateTimeParseException wrapped",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mocked ExecutionContext with schema containing { \"type\": \"string\", \"format\": \"date-time\" }"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing \"not-a-date\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same JsonNode as above (root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException wrapping DateTimeParseException is thrown",
                    "description": "When a string fails the built‑in \"date-time\" format validator, the underlying parser throws DateTimeParseException. walkAtNode should surface this as a RuntimeException (the library wraps format errors in a runtime exception)."
                }
            ]
        }
    ]
}