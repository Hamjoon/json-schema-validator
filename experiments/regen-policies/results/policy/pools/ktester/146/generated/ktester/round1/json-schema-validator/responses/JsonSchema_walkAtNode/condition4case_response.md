{
    "groups": [
        {
            "group": "Successful validation when flag is true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"John\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{ \"name\": \"John\" }"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages)",
                    "description": "The node satisfies the schema (required string property \"name\"). With validate=true the method should return a successful ValidationResult."
                }
            ]
        },
        {
            "group": "Failed validation when flag is true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "{ }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{ }"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.failure containing a ValidationMessage about missing required property 'name'",
                    "description": "The node is missing the required property defined in the schema. With validate=true the method must return a ValidationResult that contains the appropriate error message."
                }
            ]
        },
        {
            "group": "Validation skipped when flag is false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "{ }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{ }"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": "false"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages)",
                    "description": "When the validate flag is false the schema walk should not perform any validation, therefore even an invalid node must result in a successful ValidationResult."
                }
            ]
        },
        {
            "group": "Null executionContext – internal creation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"Alice\" }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{ \"name\": \"Alice\" }"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages)",
                    "description": "If the supplied ExecutionContext is null the method should create a default context internally and still validate correctly."
                }
            ]
        },
        {
            "group": "Null node – defensive handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException thrown",
                    "description": "The method is expected to throw NPE (or return a failure ValidationResult) when the node to walk is null, exercising the null‑check branch."
                }
            ]
        },
        {
            "group": "Non‑root instanceLocation tracking",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "{ \"address\": { \"city\": 123 } }"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{ \"address\": { \"city\": 123 } }"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"address\")"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.failure containing a ValidationMessage whose path is '/address/city'",
                    "description": "When walking a sub‑node, the instanceLocation should be prepended to any error paths. The schema expects \"city\" to be a string, so the error path must reflect the supplied location."
                }
            ]
        }
    ]
}