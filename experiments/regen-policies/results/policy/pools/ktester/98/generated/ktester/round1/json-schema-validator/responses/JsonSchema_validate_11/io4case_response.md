{
    "testCases": [
        {
            "group": "URL Encoded Input Scenarios",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "A correctly URL‑encoded JSON document; after decoding it matches the schema, exercising the URL_ENCODED branch without errors."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"%E4%BD%A0%E5%A5%BD\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "Throws UnsupportedEncodingException (wrapped in a ValidationException) because the decoded content is not valid JSON/YAML for the schema.",
                    "description": "Valid URL‑encoded string that decodes to non‑JSON content, triggering the decoding‑then‑deserialization error path."
                }
            ]
        },
        {
            "group": "Empty / Whitespace Input",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because the input string is empty.",
                    "description": "Empty string supplied for JSON format; deserialization should fail immediately."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"   \\n   \""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because the input contains only whitespace.",
                    "description": "Whitespace‑only input for JSON format; verifies that whitespace is not treated as valid JSON."
                }
            ]
        },
        {
            "group": "Schema Constraint Violations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30,\\\"extra\\\":\\\"value\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "A non‑empty Set<ValidationMessage> containing a message about an additional property 'extra' not being allowed.",
                    "description": "JSON contains a property that is not defined in the schema (additionalProperties = false)."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "A non‑empty Set<ValidationMessage> containing a message that required property 'age' is missing.",
                    "description": "Required property 'age' is omitted; tests the required‑property validation branch."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":null,\\\"age\\\":30}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "A non‑empty Set<ValidationMessage> containing a type‑mismatch message for property 'name' (null not allowed).",
                    "description": "Property 'name' is explicitly null while the schema expects a non‑null string."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":\\\"thirty\\\",\\\"city\\\":\\\"NY\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setFailFast(true)"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing exactly one error (either the type mismatch for 'age' or the additional property 'city'), because fail‑fast is enabled.",
                    "description": "Input has two independent violations; the custom execution context forces fail‑fast, verifying that validation stops after the first error."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":\\\"thirty\\\",\\\"city\\\":\\\"NY\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setReportAllErrors(true)"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing at least two messages (type mismatch for 'age' and additional property 'city'), because all errors are reported.",
                    "description": "Same input as above but with a customizer that ensures all errors are collected, testing the multi‑error aggregation path."
                }
            ]
        },
        {
            "group": "Custom Execution Context Edge Cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new RuntimeException(\"Customizer failure\"); }"
                        }
                    ],
                    "expected": "Throws RuntimeException with message 'Customizer failure' propagated from the executionCustomizer.",
                    "description": "The customizer itself throws an exception; verifies that such exceptions are not swallowed by the validate method."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setCustomMessage(\"myCustomMessage\")"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors) and the custom message is stored in the ExecutionContext (observable via ctx.getCustomMessage()).",
                    "description": "Customizer sets a user‑defined attribute on the ExecutionContext; the test checks that the customizer is applied without affecting validation outcome."
                }
            ]
        }
    ]
}