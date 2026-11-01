{
    "group": "putAnnotation_NullExecutionContext",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                },
                {
                    "parameter": "customizer",
                    "value": "consumer that does nothing (builder) -> {}"
                }
            ],
            "expected": "NullPointerException",
            "description": "When the provided ExecutionContext is null, the method attempts to call getAnnotations() and throws a NullPointerException."
        }
    ]
}