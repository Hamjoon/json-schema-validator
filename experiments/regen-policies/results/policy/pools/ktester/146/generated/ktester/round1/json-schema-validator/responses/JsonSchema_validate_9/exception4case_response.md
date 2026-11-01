{
    "test_cases": [
        {
            "group": "Null handling – multiple nulls",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "format",
                            "value": "null"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Both the JSON document (rootNode) and the OutputFormat are null. The method should fail fast with a NullPointerException because rootNode is validated before format."
                }
            ]
        },
        {
            "group": "Null handling – format null, customizer null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "null"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the OutputFormat is null the method must reject the call with an IllegalArgumentException, regardless of a null executionCustomizer."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer – throws NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringOutputFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new NullPointerException(\"customizer NPE\"); }"
                        }
                    ],
                    "expected": "NullPointerException with message \"customizer NPE\"",
                    "description": "If the supplied ExecutionContextCustomizer throws a NullPointerException, validate must propagate it unchanged."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer – illegal attribute key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringOutputFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setAttribute(null, \"value\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Setting an attribute with a null key is illegal for ExecutionContext; the customizer should cause an IllegalArgumentException that propagates out of validate."
                }
            ]
        },
        {
            "group": "Custom OutputFormat – RuntimeException propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { throw new RuntimeException(\"format runtime failure\"); } }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "RuntimeException with message \"format runtime failure\"",
                    "description": "When the OutputFormat implementation throws a RuntimeException, validate must let that exception bubble up unchanged."
                }
            ]
        },
        {
            "group": "Custom OutputFormat – Error propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { throw new AssertionError(\"format error\"); } }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "AssertionError with message \"format error\"",
                    "description": "If the OutputFormat throws an Error (e.g., AssertionError), validate must not catch it and should propagate it to the caller."
                }
            ]
        },
        {
            "group": "Schema with unresolved $ref – IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringOutputFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "When the JsonSchema instance contains a $ref that cannot be resolved (e.g., missing external schema), the validation process should fail with an IllegalStateException."
                }
            ]
        }
    ]
}