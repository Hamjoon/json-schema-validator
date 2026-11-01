{
    "group": "reportChildErrors",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mock ExecutionContext whose ExecutionConfig.isFailFast() returns true"
                }
            ],
            "expected": false,
            "description": "When the execution configuration is set to fail‑fast, the validator should not aggregate child errors, therefore reportChildErrors must return false."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mock ExecutionContext whose ExecutionConfig.isFailFast() returns false"
                }
            ],
            "expected": true,
            "description": "When fail‑fast is disabled, the validator should aggregate child errors, so reportChildErrors must return true."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null ExecutionContext should result in a NullPointerException because the method dereferences executionContext without a null‑check."
        }
    ]
}