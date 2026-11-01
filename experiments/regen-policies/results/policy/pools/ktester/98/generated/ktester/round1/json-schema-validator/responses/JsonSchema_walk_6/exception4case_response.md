{
    "new_test_cases": [
        {
            "group": "walk_nullExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"name\":\"John\"}"
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
                    "description": "When the ExecutionContext argument is null, the walk method should throw a NullPointerException because internal processing requires a non‑null context."
                }
            ]
        },
        {
            "group": "walk_nullExecutionContextWithCustomizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"name\":\"Alice\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If a null ExecutionContext is supplied, the consumer receives a null reference; attempting to use it (e.g., setting an attribute) should result in a NullPointerException that propagates out of walk."
                }
            ]
        }
    ]
}