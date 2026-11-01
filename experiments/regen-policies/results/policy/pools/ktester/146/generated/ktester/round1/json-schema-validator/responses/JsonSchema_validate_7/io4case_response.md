{
    "test_cases": [
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – exception propagation from OutputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"string\",\"value\":\"abc\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat that throws IllegalStateException(\"format failure\") on apply"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "The OutputFormat throws an unchecked exception during its apply call. The validate method should not catch it and the same exception must propagate to the caller."
                },
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"number\",\"value\":42}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat that throws RuntimeException(\"unexpected\") on apply"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "Another unchecked exception scenario to verify that any RuntimeException from the OutputFormat is bubbled up unchanged."
                }
            ]
        },
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – handling of JSON null node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"null\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat returning \"NULL_OK\""
                        }
                    ],
                    "expected": "NULL_OK",
                    "description": "A JsonNode that represents JSON null is still a non‑null Java object. The method should delegate normally and return the value produced by the OutputFormat."
                }
            ]
        },
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – format returns complex object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"array\",\"items\":[1,2,3]}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat returning List.of(\"A\",\"B\",\"C\")"
                        }
                    ],
                    "expected": "[\"A\",\"B\",\"C\"]",
                    "description": "The generic type T can be any object. Verify that the method returns the exact object supplied by the OutputFormat (a List in this case)."
                }
            ]
        },
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – OutputFormat invoked exactly once",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"object\",\"properties\":{}}"
                        },
                        {
                            "parameter": "format",
                            "value": "SpyOutputFormat that records number of apply calls"
                        }
                    ],
                    "expected": "applyCallCount == 1",
                    "description": "Using a spy (e.g., Mockito) ensure that the delegated validate method calls the OutputFormat's apply method exactly once."
                }
            ]
        },
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – empty object node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"object\",\"properties\":{}}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat returning \"EMPTY_OBJ_OK\""
                        }
                    ],
                    "expected": "EMPTY_OBJ_OK",
                    "description": "Validate that an empty JSON object node is handled like any other node and the OutputFormat's result is returned unchanged."
                }
            ]
        },
        {
            "group": "validate(ExecutionContext, JsonNode, OutputFormat) – executionContext with custom state",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "MockExecutionContext with attribute \"mode\"=\"STRICT\""
                        },
                        {
                            "parameter": "node",
                            "value": "MockJsonNode{\"type\":\"string\",\"value\":\"test\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "MockOutputFormat that returns executionContext.getAttribute(\"mode\")"
                        }
                    ],
                    "expected": "STRICT",
                    "description": "The OutputFormat may inspect the ExecutionContext. Verify that the same ExecutionContext instance is passed through the delegation, allowing the format to read custom attributes."
                }
            ]
        }
    ]
}