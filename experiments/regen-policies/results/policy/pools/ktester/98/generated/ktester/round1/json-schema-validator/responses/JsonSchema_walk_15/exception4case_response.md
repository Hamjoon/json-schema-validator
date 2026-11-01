{
    "new_test_cases": [
        {
            "group": "OutputFormat throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"key\\\":\\\"value\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { throw new RuntimeException(\"output failure\"); } }"
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
                    "description": "When the supplied OutputFormat throws a RuntimeException while processing validation messages, walk should propagate that exception unchanged."
                }
            ]
        },
        {
            "group": "Deserialize throws UnsupportedEncodingException due to malformed URL encoding",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"%ZZ\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL"
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
                    "expected": "UnsupportedEncodingException",
                    "description": "If the InputFormat requires URL decoding and the input contains an invalid percent‑encoding sequence, the deserialize step should throw UnsupportedEncodingException which must propagate out of walk."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer throws checked exception wrapped in RuntimeException",
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
                            "value": "ctx -> { try { throw new java.io.IOException(\"io failure\"); } catch (java.io.IOException e) { throw new RuntimeException(e); } }"
                        }
                    ],
                    "expected": "RuntimeException caused by IOException with message \"io failure\"",
                    "description": "When a custom ExecutionContextCustomizer encounters a checked exception and wraps it in a RuntimeException, walk must let that RuntimeException bubble up."
                }
            ]
        }
    ]
}