{
    "test_cases": [
        {
            "group": "Empty JSON string – deserialization failure",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "new ExecutionContext()" },
                        { "parameter": "input", "value": "\"\"" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true },
                        { "parameter": "executionCustomizer", "value": "ctx -> {}" }
                    ],
                    "expected": "Throws JsonParseException (or IOException) wrapped by ValidationException",
                    "description": "An empty string is not valid JSON; the deserialize step should fail and the walk method must propagate the parsing exception."
                }
            ]
        },
        {
            "group": "Null input string – handled gracefully",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "new ExecutionContext()" },
                        { "parameter": "input", "value": "null" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true },
                        { "parameter": "executionCustomizer", "value": "ctx -> {}" }
                    ],
                    "expected": "Throws NullPointerException (or IllegalArgumentException) indicating that input may not be null",
                    "description": "Passing a null string to the method should cause an immediate failure before any deserialization occurs."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer throws RuntimeException – propagation",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "new ExecutionContext()" },
                        { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\"}\"" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true },
                        { "parameter": "executionCustomizer", "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }" }
                    ],
                    "expected": "RuntimeException with message \"customizer failure\" propagated to the caller",
                    "description": "If the customizer throws an unchecked exception, walk should not swallow it; the exception must bubble up."
                }
            ]
        },
        {
            "group": "Fail‑fast configuration – stops after first validation error",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "ExecutionContext.builder().build()" },
                        { "parameter": "input", "value": "\"{\\\"age\\\":\\\"twenty\\\", \\\"extra\\\":123}\"" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true },
                        { "parameter": "executionCustomizer", "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true))" }
                    ],
                    "expected": "ValidationResult containing exactly one ValidationMessage (the first error encountered)",
                    "description": "With failFast enabled via the custom config, validation should abort after the first schema violation, so only a single error is reported even though the JSON has multiple issues."
                }
            ]
        },
        {
            "group": "Valid URL‑encoded input – successful validation",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "new ExecutionContext()" },
                        { "parameter": "input", "value": "\"%7B%22name%22%3A%22John%22%7D\"" },
                        { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
                        { "parameter": "validate", "value": true },
                        { "parameter": "executionCustomizer", "value": "ctx -> {}" }
                    ],
                    "expected": "ValidationResult indicating successful validation (no errors)",
                    "description": "A correctly percent‑encoded JSON string should be decoded, deserialized and validated without errors."
                }
            ]
        },
        {
            "group": "Validate flag false with custom ExecutionContext – validation still skipped",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "ExecutionContext.builder().setVersion(VersionFlag.V202012).build()" },
                        { "parameter": "input", "value": "\"{\\\"age\\\":\\\"twenty\\\"}\"" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": false },
                        { "parameter": "executionCustomizer", "value": "ctx -> {}" }
                    ],
                    "expected": "ValidationResult with empty error set (validation skipped) while the custom ExecutionContext is still used internally",
                    "description": "Even when a non‑default ExecutionContext (different spec version) is supplied, setting validate to false must cause walk to bypass all validators."
                }
            ]
        }
    ]
}