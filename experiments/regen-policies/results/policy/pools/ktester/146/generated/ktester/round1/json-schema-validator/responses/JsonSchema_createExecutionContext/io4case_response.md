{
    "testCases": [
        {
            "group": "createExecutionContext copies locale",
            "cases": [
                {
                    "input": [],
                    "expected": "ExecutionContext returned contains an ExecutionConfig whose locale exactly matches the locale set in the SchemaValidatorsConfig (e.g., Locale.FRANCE). All other config values are also copied, but the focus of this test is the locale field.",
                    "description": "Verify that when the SchemaValidatorsConfig has a specific non‑default Locale, createExecutionContext copies that locale into the new ExecutionConfig."
                }
            ]
        },
        {
            "group": "createExecutionContext copies formatAssertionsEnabled flag",
            "cases": [
                {
                    "input": [],
                    "expected": "ExecutionContext returned contains an ExecutionConfig with formatAssertionsEnabled set to false when the SchemaValidatorsConfig has formatAssertionsEnabled set to false (and likewise true when true).",
                    "description": "Confirm that the boolean flag formatAssertionsEnabled is transferred unchanged from SchemaValidatorsConfig to ExecutionConfig."
                }
            ]
        },
        {
            "group": "createExecutionContext copies failFast flag",
            "cases": [
                {
                    "input": [],
                    "expected": "ExecutionContext returned contains an ExecutionConfig whose failFast flag equals the value defined in SchemaValidatorsConfig (tested with true and false).",
                    "description": "Ensure that the failFast setting is correctly propagated to the ExecutionConfig."
                }
            ]
        },
        {
            "group": "createExecutionContext returns new instance each call",
            "cases": [
                {
                    "input": [],
                    "expected": "Two successive calls to createExecutionContext on the same JsonSchema instance produce two distinct ExecutionContext objects (different object references).",
                    "description": "Validate that createExecutionContext does not cache or reuse ExecutionContext instances; each invocation yields a fresh object."
                }
            ]
        },
        {
            "group": "createExecutionContext customizer modifies ExecutionConfig",
            "cases": [
                {
                    "input": [],
                    "expected": "When a non‑null ExecutionContextCustomizer is supplied, its customize method can change the ExecutionConfig (e.g., set a different locale). After createExecutionContext returns, the ExecutionContext reflects those modifications.",
                    "description": "Test that the customizer is invoked after the default config copy and that any changes it makes to the ExecutionContext are retained."
                }
            ]
        },
        {
            "group": "createExecutionContext customizer receives correct ValidationContext",
            "cases": [
                {
                    "input": [],
                    "expected": "The ValidationContext instance passed to the customizer's customize method is the same instance that JsonSchema holds (validationContext). No new ValidationContext is created.",
                    "description": "Ensure that the customizer receives the original ValidationContext associated with the JsonSchema, allowing it to inspect or use the same context."
                }
            ]
        }
    ]
}