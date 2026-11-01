{
    "group": "createExecutionContext - locale propagation",
    "cases": [
        {
            "input": [],
            "expected": "ExecutionContext is returned whose ExecutionConfig.locale equals the Locale set in the SchemaValidatorsConfig (e.g., Locale.GERMAN). All other flags (formatAssertionsEnabled, failFast) are also copied correctly.",
            "description": "Verify that a non‑default locale configured in ValidationContext.getConfig() is correctly transferred to the newly created ExecutionContext."
        }
    ]
}