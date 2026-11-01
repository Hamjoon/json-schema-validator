{
    "test_cases": [
        {
            "group": "Null ValidationContext Handling",
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
                            "parameter": "format",
                            "value": "outputFormatThatUsesValidationContext"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException is thrown when format.customize attempts to access the JsonSchema's validationContext which is null. No further calls (customizer, internal validate, format.format) are performed.",
                    "description": "Simulate a JsonSchema instance whose internal validationContext field is null (e.g., via reflection). The OutputFormat.customize method accesses the supplied validationContext; because it is null, a NPE occurs. The method must propagate this exception immediately."
                }
            ]
        },
        {
            "group": "Null ExecutionContext After Customize",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": null
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormatNoOpCustomize"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException is thrown by the internal validate(executionContext, node) call because the provided ExecutionContext is null. The exception occurs after format.customize has been invoked successfully.",
                    "description": "The OutputFormat.customize implementation is a no‑op that tolerates a null ExecutionContext. After customization, the method proceeds to the internal validation step, which dereferences the ExecutionContext and throws NPE. The NPE should propagate out of the validate method."
                }
            ]
        },
        {
            "group": "Internal Validation RuntimeException Propagation",
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
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "IllegalStateException thrown from the internal validate(executionContext, node) method propagates out of the outer validate method. Neither format.format nor any further processing is performed.",
                    "description": "Create a subclass or spy of JsonSchema where the protected validate(ExecutionContext, JsonNode) method is overridden to throw an IllegalStateException. The outer validate method should not catch this exception; it must bubble up unchanged."
                }
            ]
        }
    ]
}