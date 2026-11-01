{
    "newTestCases": [
        {
            "group": "initializeValidators - partial preload before exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException propagated, validatorsLoaded remains false, preloadJsonSchema() invoked on validators preceding the failing one",
                    "description": "When getValidators() returns a list of several validators and the third validator throws a RuntimeException during preload, the method must propagate the exception, stop further iteration, and must not set validatorsLoaded to true. Validators before the failure should have their preloadJsonSchema() called exactly once."
                }
            ]
        },
        {
            "group": "initializeValidators - retry after previous failure",
            "cases": [
                {
                    "input": [],
                    "expected": "First call propagates exception and leaves validatorsLoaded false; second call succeeds, invoking preloadJsonSchema() on all validators and setting validatorsLoaded to true",
                    "description": "Simulate a first invocation where a validator throws; verify that validatorsLoaded stays false. Then invoke initializeValidators() again with the same (now non‑failing) validators list and confirm that preload is attempted again and validatorsLoaded becomes true."
                }
            ]
        },
        {
            "group": "initializeValidators - multiple validators each called once",
            "cases": [
                {
                    "input": [],
                    "expected": "validatorsLoaded == true and preloadJsonSchema() invoked exactly once on each of the 5 validators",
                    "description": "Provide a list containing five distinct JsonValidator mocks. Ensure that initializeValidators() iterates over the entire list, calls preloadJsonSchema() on each validator exactly once, and finally marks validatorsLoaded as true."
                }
            ]
        },
        {
            "group": "initializeValidators - already loaded but validator list changed",
            "cases": [
                {
                    "input": [],
                    "expected": "validatorsLoaded remains true and no additional preloadJsonSchema() calls are made for the newly added validator",
                    "description": "After a successful first call (validatorsLoaded == true), modify the schema so that getValidators() now returns an additional validator. A subsequent call to initializeValidators() must skip the preload loop entirely because validatorsLoaded is already true, leaving the new validator untouched."
                }
            ]
        },
        {
            "group": "initializeValidators - getValidators returns null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown, validatorsLoaded stays false",
                    "description": "Force getValidators() to return null. The for‑each loop will throw a NullPointerException; the method should not catch it, allowing the exception to propagate and leaving validatorsLoaded unchanged."
                }
            ]
        }
    ]
}