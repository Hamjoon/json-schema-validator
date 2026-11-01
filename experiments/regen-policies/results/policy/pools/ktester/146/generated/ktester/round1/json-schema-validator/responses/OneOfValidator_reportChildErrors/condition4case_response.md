{
    "group": "reportChildErrors",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextFailFastTrue"
                }
            ],
            "expected": false,
            "description": "When the ExecutionConfig reports failFast = true, the validator should not report child errors (method returns false)."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextFailFastFalse"
                }
            ],
            "expected": true,
            "description": "When the ExecutionConfig reports failFast = false, the validator should aggregate child errors (method returns true)."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null ExecutionContext should result in a NullPointerException because the method dereferences the argument."
        }
    ]
}