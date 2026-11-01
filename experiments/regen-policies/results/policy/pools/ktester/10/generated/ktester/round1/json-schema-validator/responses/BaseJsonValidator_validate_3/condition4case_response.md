{
    "group": "BaseJsonValidator.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "format",
                    "value": "mockOutputFormat (customize does nothing, format returns \"RESULT-NULL-CUSTOMIZER\")"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "null"
                }
            ],
            "expected": "Returns \"RESULT-NULL-CUSTOMIZER\". verify that OutputFormat.customize(executionContext, validationContext) is called exactly once, and ExecutionContextCustomizer.customize is NOT called.",
            "description": "When the executionCustomizer argument is null, the method should skip the customizer step, still invoke format.customize, and return the value produced by format.format."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "format",
                    "value": "mockOutputFormat (customize does nothing, format returns \"RESULT-WITH-CUSTOMIZER\")"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "mockExecutionCustomizer (customize records invocation)"
                }
            ],
            "expected": "Returns \"RESULT-WITH-CUSTOMIZER\". verify that OutputFormat.customize is called once, ExecutionContextCustomizer.customize is called once with the same executionContext and validationContext, and format.format is called once returning the expected result.",
            "description": "When a non‑null ExecutionContextCustomizer is supplied, the method must invoke its customize method after the format.customize call, then continue to validation and return the formatted result."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "format",
                    "value": "mockOutputFormat (customize throws RuntimeException \"customize-failure\")"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "null"
                }
            ],
            "expected": "RuntimeException with message \"customize-failure\" is propagated.",
            "description": "If OutputFormat.customize throws an exception, the method should not swallow it; the exception propagates to the caller."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "format",
                    "value": "mockOutputFormat (customize succeeds, format throws IllegalStateException \"format-failure\")"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "mockExecutionCustomizer"
                }
            ],
            "expected": "IllegalStateException with message \"format-failure\" is propagated.",
            "description": "If OutputFormat.format throws an exception after validation, the method should propagate that exception."
        }
    ]
}