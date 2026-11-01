{
    "test_cases": [
        {
            "group": "createExecutionContext when validationContext is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The JsonSchema instance has its internal validationContext field set to null. Invoking createExecutionContext() should throw a NullPointerException because the method tries to call validationContext.getConfig()."
                }
            ]
        },
        {
            "group": "createExecutionContext when config is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The ValidationContext returns null from getConfig(). Calling createExecutionContext() must raise a NullPointerException when the method attempts to access methods on the null config object."
                }
            ]
        },
        {
            "group": "createExecutionContext when customizer throws runtime exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "A non‑null ExecutionContextCustomizer is provided via SchemaValidatorsConfig. Its customize(...) method deliberately throws a RuntimeException. The exception should propagate out of createExecutionContext() without being caught."
                }
            ]
        }
    ]
}