{
  "testCases": [
    {
      "group": "createExecutionContext without customizer",
      "cases": [
        {
          "input": [],
          "expected": "ExecutionContext is returned whose ExecutionConfig copies locale, formatAssertionsEnabled and failFast flags from the SchemaValidatorsConfig. Since getExecutionContextCustomizer() returns null, no customizer method is invoked.",
          "description": "Validate that createExecutionContext copies the default execution configuration values from the ValidationContext's SchemaValidatorsConfig and returns a fresh ExecutionContext when no ExecutionContextCustomizer is configured."
        }
      ]
    },
    {
      "group": "createExecutionContext with customizer",
      "cases": [
        {
          "input": [],
          "expected": "ExecutionContext is returned with the same copied ExecutionConfig values as above, and the provided ExecutionContextCustomizer's customize method is invoked exactly once with the newly created ExecutionContext and the current ValidationContext.",
          "description": "Validate that when the SchemaValidatorsConfig provides a non‑null ExecutionContextCustomizer, createExecutionContext still copies the config values correctly and then calls the customizer to allow further modifications of the ExecutionContext."
        }
      ]
    }
  ]
}