{
    "group": "reportChildErrors",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextConfigNull"
                }
            ],
            "expected": "NullPointerException",
            "description": "When ExecutionContext.getExecutionConfig() returns null, the method dereferences the result and throws a NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextConfigThrows"
                }
            ],
            "expected": "RuntimeException",
            "description": "If ExecutionConfig.isFailFast() throws a RuntimeException, the exception should propagate out of reportChildErrors."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextConfigSideEffectTrue"
                }
            ],
            "expected": false,
            "description": "ExecutionConfig.isFailFast() returns true and increments a counter as a side‑effect; reportChildErrors should return false and the side‑effect should be observed exactly once."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextConfigSideEffectFalse"
                }
            ],
            "expected": true,
            "description": "ExecutionConfig.isFailFast() returns false and increments a counter as a side‑effect; reportChildErrors should return true and the side‑effect should be observed exactly once."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "spyExecutionContextConfigTrue"
                }
            ],
            "expected": false,
            "description": "Using a spy for ExecutionContext and ExecutionConfig where isFailFast() returns true; verify that getExecutionConfig() and isFailFast() are each invoked exactly once and the method returns false."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "spyExecutionContextConfigFalse"
                }
            ],
            "expected": true,
            "description": "Using a spy for ExecutionContext and ExecutionConfig where isFailFast() returns false; verify that getExecutionConfig() and isFailFast() are each invoked exactly once and the method returns true."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextFailFastTrueRepeated"
                }
            ],
            "expected": false,
            "description": "Call reportChildErrors twice with the same ExecutionContext where failFast = true; both invocations should consistently return false."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextFailFastFalseRepeated"
                }
            ],
            "expected": true,
            "description": "Call reportChildErrors twice with the same ExecutionContext where failFast = false; both invocations should consistently return true."
        }
    ]
}