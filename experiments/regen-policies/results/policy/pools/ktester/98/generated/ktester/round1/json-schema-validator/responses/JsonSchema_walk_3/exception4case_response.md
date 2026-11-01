{
    "test_cases": [
        {
            "group": "walk_invalidRefSyntax",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext() for a schema containing a malformed $ref (e.g., \"$ref\": \"http://[invalid-uri]\")"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing any JSON (e.g., {\"any\":\"value\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) indicating that the $ref URI is malformed",
                    "description": "The schema contains a $ref with an invalid URI syntax. walk should detect the malformed reference during resolution and throw an appropriate exception rather than returning a ValidationResult."
                }
            ]
        },
        {
            "group": "walk_unresolvableRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext() for a schema that contains a $ref to a non‑existent definition (e.g., \"$ref\": \"#/components/schemas/Unknown\")"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing any JSON (e.g., {\"any\":\"value\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) stating that the reference could not be resolved",
                    "description": "When a $ref points to a fragment that does not exist in the schema, walk must fail fast and throw an exception indicating the unresolved reference."
                }
            ]
        },
        {
            "group": "walk_recursiveAnchorDepthExceeded",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext with recursive anchor enabled and a recursion depth limit set to a low value (e.g., via ValidationContext#setMaxRecursionDepth(1))"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode that triggers recursive validation (e.g., {\"$recursiveRef\":\"#\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "StackOverflowError or JsonSchemaException indicating recursion depth exceeded",
                    "description": "The schema defines a recursive anchor and the instance references itself. With a low recursion‑depth limit, walk should detect the overflow condition and throw an exception instead of looping indefinitely."
                }
            ]
        },
        {
            "group": "walk_executionContextMissingValidationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext instance whose internal ValidationContext is null (constructed manually for the test)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a valid instance (e.g., {\"name\":\"Bob\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalStateException (or NullPointerException) because the ExecutionContext does not contain a required ValidationContext",
                    "description": "walk relies on a non‑null ValidationContext inside the ExecutionContext. Supplying an ExecutionContext with a missing ValidationContext should cause an immediate exception."
                }
            ]
        },
        {
            "group": "walk_unsupportedEncodingDuringRefResolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext() for a schema that contains a $ref with percent‑encoded characters requiring an unsupported charset (e.g., \"%FF%FE\")"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing any JSON (e.g., {\"any\":\"value\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "UnsupportedEncodingException wrapped in JsonSchemaException",
                    "description": "During $ref fragment decoding, URLDecoder may throw UnsupportedEncodingException for an unknown charset. walk should propagate this as a wrapped JsonSchemaException."
                }
            ]
        }
    ]
}