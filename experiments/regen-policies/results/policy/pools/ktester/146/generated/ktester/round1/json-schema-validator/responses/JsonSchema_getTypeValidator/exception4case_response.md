{
    "test_cases": [
        {
            "group": "validators null – getValidators throws IllegalArgumentException",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException propagated from getValidators; typeValidator remains null",
                    "description": "When `validators` is null, `getTypeValidator()` calls `getValidators()`. The overridden `getValidators()` throws an `IllegalArgumentException` with a custom message. The method must not catch it; the exception should bubble up unchanged and `typeValidator` should stay null."
                }
            ]
        },
        {
            "group": "validators null – getValidators throws IllegalStateException",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException propagated from getValidators; typeValidator remains null",
                    "description": "Simulate a failure during lazy loading where `getValidators()` throws an `IllegalStateException`. `getTypeValidator()` should propagate this unchecked exception without alteration."
                }
            ]
        },
        {
            "group": "validators null – getValidators throws AssertionError (Error subclass)",
            "cases": [
                {
                    "input": [],
                    "expected": "AssertionError propagated from getValidators; typeValidator remains null",
                    "description": "Even though `Error` is not an `Exception`, the method must not swallow it. The overridden `getValidators()` throws an `AssertionError`; `getTypeValidator()` should let the error propagate."
                }
            ]
        }
    ]
}