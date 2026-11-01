{
    "groups": [
        {
            "group": "Unresolved $ref leads to JsonSchemaException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"$ref\": \"#/nonexistent\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) thrown because the $ref cannot be resolved to any subschema.",
                    "description": "The schema contains a $ref that points to a fragment that does not exist. walk should attempt to resolve the reference and fail, propagating an exception."
                }
            ]
        },
        {
            "group": "Invalid $id URI causing UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"$id\": \"http://example.com/schema%ZZ\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "UnsupportedEncodingException thrown while decoding the $id URI.",
                    "description": "The $id contains an invalid percent‑encoding sequence. During walk the schema id is URL‑decoded, which should raise an UnsupportedEncodingException."
                }
            ]
        },
        {
            "group": "Circular $ref without recursive anchor causing StackOverflowError",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"$ref\": \"#/definitions/self\" , \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } } }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "StackOverflowError (or similar recursion error) because the schema contains a self‑referencing $ref that is not marked as a recursive anchor.",
                    "description": "When walk follows the $ref chain it will recurse indefinitely, eventually overflowing the stack. The test verifies that such a situation results in a runtime error."
                }
            ]
        },
        {
            "group": "OutputFormat throws checked IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"type\": \"integer\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "IOExceptionThrowingOutputFormat (throws java.io.IOException on format call)"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException (or unchecked wrapper) propagating the IOException thrown by the OutputFormat.",
                    "description": "A custom OutputFormat deliberately throws a checked IOException during its processing. walk must not swallow the exception; it should be wrapped or re‑thrown as an unchecked exception."
                }
            ]
        },
        {
            "group": "MissingNode argument causes IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "MissingNode.INSTANCE"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException (or NullPointerException) thrown because the supplied JsonNode is a MissingNode, which is not a valid instance for validation.",
                    "description": "When the caller passes Jackson's MissingNode (representing a non‑existent field) walk should reject it and raise an appropriate exception."
                }
            ]
        },
        {
            "group": "ExecutionContext creation failure leads to IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"type\": \"object\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalStateException thrown from createExecutionContext() when the schema has not been initialized properly (e.g., missing validation context).",
                    "description": "If the JsonSchema instance is in an invalid state such that createExecutionContext() cannot construct a usable ExecutionContext, walk should propagate the IllegalStateException."
                }
            ]
        }
    ]
}