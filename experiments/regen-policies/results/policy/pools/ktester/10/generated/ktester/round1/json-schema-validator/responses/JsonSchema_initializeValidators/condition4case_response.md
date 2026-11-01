{
    "group": "initializeValidators - first invocation loads validators",
    "cases": [
        {
            "input": [],
            "expected": "All validators' preloadJsonSchema() methods are invoked exactly once and JsonSchema.validatorsLoaded becomes true",
            "description": "When validatorsLoaded is false, initializeValidators() should iterate over the list returned by getValidators(), call preloadJsonSchema() on each validator, and finally set validatorsLoaded to true."
        },
        {
            "input": [],
            "expected": "preloadJsonSchema() is not invoked for any validator and validatorsLoaded remains true",
            "description": "When validatorsLoaded is already true, initializeValidators() must skip the preload loop entirely and leave the flag unchanged."
        },
        {
            "input": [],
            "expected": "RuntimeException is propagated; preloadJsonSchema() is invoked on the first validator, the exception aborts the method, and validatorsLoaded stays false",
            "description": "If a validator throws an exception during preload, initializeValidators() should not set validatorsLoaded to true, allowing a retry on a later call."
        }
    ]
}