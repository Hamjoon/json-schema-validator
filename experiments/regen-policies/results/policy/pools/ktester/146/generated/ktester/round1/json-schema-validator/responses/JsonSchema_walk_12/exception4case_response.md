{
    "test_cases": [
        {
            "group": "Null OutputFormat – should throw NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The walk method requires a non‑null OutputFormat; passing null must result in a NPE before any validation is performed."
                }
            ]
        },
        {
            "group": "Malformed $ref URI – should throw IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "jsonNodeWithMalformedRef" 
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When a schema contains a $ref with an invalid percent‑encoding (e.g., \"%ZZ\"), URLDecoder.decode throws an IllegalArgumentException. The walk method should propagate this as an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "UnsupportedEncodingException from $ref decoding – should propagate as IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "jsonNodeWithUnsupportedEncodingRef"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If a $ref contains characters that cannot be decoded using the platform default charset, URLDecoder.decode may throw UnsupportedEncodingException wrapped in an IllegalArgumentException. The walk method must surface this as an IllegalArgumentException."
                }
            ]
        }
    ]
}