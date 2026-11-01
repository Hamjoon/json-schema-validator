{
    "groups": [
        {
            "group": "Validation failure returns messages",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"age\\\":\\\"twenty\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "ValidationResult containing at least one ValidationMessage (type mismatch for property 'age')",
                    "description": "The schema requires 'age' to be a number. Supplying a string should cause validation to fail, resulting in a ValidationResult with a non‑empty set of ValidationMessage objects."
                }
            ]
        },
        {
            "group": "Null outputFormat causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"valid\\\":true}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null OutputFormat should be rejected early, causing a NullPointerException before any deserialization or validation occurs."
                }
            ]
        },
        {
            "group": "Null inputFormat causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"x\\\":1}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the InputFormat argument is null, the method cannot decide how to deserialize the string and must throw a NullPointerException."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"a\\\":1}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
                        }
                    ],
                    "expected": "RuntimeException with message \"customizer failure\"",
                    "description": "When the supplied ExecutionContextCustomizer throws an unchecked exception, walk should propagate that exception without swallowing it."
                }
            ]
        },
        {
            "group": "Empty input string causes JsonProcessingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonProcessingException (or IOException) thrown by deserialize",
                    "description": "An empty string is not valid JSON; the deserialize step should fail and the exception should bubble up from walk."
                }
            ]
        },
        {
            "group": "Whitespace‑only input string causes JsonProcessingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"   \\n\\t  \""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonProcessingException (or IOException) thrown by deserialize",
                    "description": "A string containing only whitespace cannot be parsed as JSON; the method should raise a parsing exception."
                }
            ]
        },
        {
            "group": "Null input string causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<ValidationResult>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null input string should result in a NullPointerException before deserialization is attempted."
                }
            ]
        }
    ]
}