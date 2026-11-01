{
    "testCases": [
        {
            "group": "walk_nullExecutionContext_throwsNPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "input",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "OutputFormat.SET_OF_MESSAGES"
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
                    "expected": "NullPointerException (executionContext is null)",
                    "description": "Passing a null ExecutionContext should cause the method to throw a NullPointerException before any deserialization or validation occurs."
                }
            ]
        },
        {
            "group": "walk_nullInputFormat_throwsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "OutputFormat.SET_OF_MESSAGES"
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
                    "expected": "IllegalArgumentException (inputFormat must not be null)",
                    "description": "If the InputFormat argument is null the method should reject the call with an IllegalArgumentException rather than attempting deserialization."
                }
            ]
        },
        {
            "group": "walk_nullOutputFormat_throwsNullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
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
                    "expected": "NullPointerException (outputFormat is null)",
                    "description": "A null OutputFormat cannot be applied to the validation result set; the method should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "walk_executionCustomizerThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "OutputFormat.SET_OF_MESSAGES"
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
                    "description": "When the supplied ExecutionContextCustomizer throws an exception, walk should propagate that exception (wrapped or raw) and abort processing."
                }
            ]
        },
        {
            "group": "walk_outputFormatThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> msgs) { throw new RuntimeException(\"output failure\"); } }"
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
                    "expected": "RuntimeException with message \"output failure\"",
                    "description": "If the OutputFormat implementation throws an exception while converting the validation messages, the walk method must let that exception propagate."
                }
            ]
        },
        {
            "group": "walk_unsupportedInputFormat_throwsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "<root><name>John</name></root>"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.XML"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "OutputFormat.SET_OF_MESSAGES"
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
                    "expected": "IllegalArgumentException (unsupported InputFormat: XML)",
                    "description": "The library does not support XML deserialization; passing InputFormat.XML should result in an IllegalArgumentException before any walking occurs."
                }
            ]
        },
        {
            "group": "walk_urlEncodedInputUnsupportedEncoding_throwsUnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "%E0%A4%A"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "OutputFormat.SET_OF_MESSAGES"
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
                    "expected": "UnsupportedEncodingException (malformed percent‑encoding)",
                    "description": "When InputFormat.URL_ENCODED is used, the method decodes the string. An invalid percent‑encoding sequence should cause an UnsupportedEncodingException to be thrown."
                }
            ]
        }
    ]
}