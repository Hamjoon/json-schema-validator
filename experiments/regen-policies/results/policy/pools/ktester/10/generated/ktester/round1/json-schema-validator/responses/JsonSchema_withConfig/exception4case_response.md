{
    "test_cases": [
        {
            "group": "withConfig_nullValidationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "anyConfigInstance"
                        }
                    ],
                    "expected": "throws NullPointerException",
                    "description": "The JsonSchema instance has its internal `validationContext` field set to `null`. Invoking `withConfig` attempts to call `this.getValidationContext().getConfig()`, which results in a NullPointerException because `getValidationContext()` returns null. The test verifies that the method does not catch this exception."
                }
            ]
        },
        {
            "group": "withConfig_currentConfigEqualsThrows",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "differentConfigInstance"
                        }
                    ],
                    "expected": "throws RuntimeException",
                    "description": "The current `SchemaValidatorsConfig` stored in the ValidationContext overrides `equals(Object)` to deliberately throw a `RuntimeException`. When `withConfig` executes `!this.getValidationContext().getConfig().equals(config)`, the overridden `equals` is invoked and the exception propagates. The test ensures that such unexpected exceptions are not swallowed by the method."
                }
            ]
        }
    ]
}