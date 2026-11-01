{
    "groups": [
        {
            "group": "createExecutionContext - null validationContext",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown because validationContext is null.",
                    "description": "Verify that when the JsonSchema instance has a null validationContext, calling createExecutionContext() results in a NullPointerException."
                }
            ]
        },
        {
            "group": "createExecutionContext - null config",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown because validationContext.getConfig() returns null.",
                    "description": "Verify that when validationContext.getConfig() returns null, createExecutionContext() throws a NullPointerException."
                }
            ]
        }
    ]
}