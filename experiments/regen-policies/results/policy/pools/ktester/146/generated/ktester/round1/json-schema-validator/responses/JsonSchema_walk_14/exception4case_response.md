{
    "test_cases": [
        {
            "group": "Null InputFormat – should raise IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "IllegalArgumentException (or NullPointerException) indicating that inputFormat must not be null",
                    "description": "Passing a null InputFormat should be rejected by the method before deserialization, resulting in an immediate exception."
                }
            ]
        },
        {
            "group": "Invalid YAML input – deserialization failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name: John\\nage: thirty\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "IOException (or JsonParseException) wrapped in ValidationException indicating YAML parsing error",
                    "description": "The YAML string is syntactically invalid (non‑numeric value for an integer field). Deserialization should fail and the exception must propagate."
                }
            ]
        },
        {
            "group": "Malformed URL‑encoded input – IllegalArgumentException from URLDecoder",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"%7B%22name%22%3A%22John\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "IllegalArgumentException indicating malformed percent‑encoding",
                    "description": "When the URL‑encoded string ends with an incomplete percent sequence, URLDecoder throws IllegalArgumentException. The walk method should let this exception bubble up."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer throws AssertionError – propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new AssertionError(\"customizer assertion failure\"); }"
                        }
                    ],
                    "expected": "AssertionError with message \"customizer assertion failure\" propagated to the caller",
                    "description": "If the customizer throws an Error (e.g., AssertionError), the walk method must not catch it; the error should propagate unchanged."
                }
            ]
        }
    ]
}