{
    "test_cases": [
        {
            "group": "initializeValidators - getValidators throws unchecked exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException (or any unchecked exception) propagates; validatorsLoaded flag is set to true before the exception is thrown.",
                    "description": "When the internal getValidators() method throws an unchecked exception (e.g., IllegalStateException), initializeValidators() should not catch it. The flag validatorsLoaded is assigned true prior to the call, so after the exception the flag remains true."
                }
            ]
        },
        {
            "group": "initializeValidators - concurrent modification during iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "ConcurrentModificationException propagates; validatorsLoaded flag is true when the exception occurs.",
                    "description": "If the validators map is modified concurrently while the for‑each loop iterates over getValidators().values(), a ConcurrentModificationException is thrown. The method should let this exception bubble up, with validatorsLoaded already set to true."
                }
            ]
        },
        {
            "group": "initializeValidators - validator throws Error during preload",
            "cases": [
                {
                    "input": [],
                    "expected": "Error (e.g., AssertionError) propagates; validatorsLoaded flag is true before the error propagates.",
                    "description": "When a validator's preloadJsonSchema() method throws an Error rather than an Exception, initializeValidators() must not catch it. The flag validatorsLoaded is set to true before invoking preloadJsonSchema(), so the flag stays true after the error."
                }
            ]
        }
    ]
}