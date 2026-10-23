{
    "groups": [
        {
            "group": "Null internal schemas list",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown when `preloadJsonSchema()` attempts to iterate over a null `schemas` field.",
                    "description": "Using reflection, set the private `schemas` field of a `OneOfValidator` instance to null. Invoking `preloadJsonSchema()` should result in a NullPointerException because the for‑each loop cannot iterate over a null collection."
                }
            ]
        },
        {
            "group": "Error propagation",
            "cases": [
                {
                    "input": [],
                    "expected": "AssertionError thrown by a schema's `initializeValidators()` propagates out of `preloadJsonSchema()`.",
                    "description": "Add a mock `JsonSchema` to the validator's `schemas` list and configure its `initializeValidators()` method to throw an `AssertionError`. After calling `preloadJsonSchema()`, the same `AssertionError` should be observed, confirming that the method does not catch `Error` types."
                }
            ]
        }
    ]
}