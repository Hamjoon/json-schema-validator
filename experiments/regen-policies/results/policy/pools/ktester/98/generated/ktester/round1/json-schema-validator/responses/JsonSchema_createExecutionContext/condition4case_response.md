{
  "group": "createExecutionContext - copy defaults and customizer handling",
  "cases": [
    {
      "input": [],
      "expected": "ExecutionContext is returned whose ExecutionConfig has locale, formatAssertionsEnabled and failFast identical to those of the SchemaValidatorsConfig returned by validationContext.getConfig(). No customizer is invoked because config.getExecutionContextCustomizer() is null.",
      "description": "Verify that when the SchemaValidatorsConfig does not provide an ExecutionContextCustomizer, createExecutionContext() copies the locale, formatAssertionsEnabled and failFast flags correctly and returns the new ExecutionContext unchanged."
    },
    {
      "input": [],
      "expected": "ExecutionContext is returned with the same default values as above, and the supplied ExecutionContextCustomizer's customize method is invoked exactly once with the newly created ExecutionContext and the JsonSchema's ValidationContext. The customizer flips the failFast flag, so after customization executionContext.getExecutionConfig().isFailFast() is the opposite of the original config value.",
      "description": "Verify that when the SchemaValidatorsConfig provides a non‑null ExecutionContextCustomizer, createExecutionContext() calls customize(...) and that the customizer can mutate the ExecutionContext (e.g., toggle failFast)."
    },
    {
      "input": [],
      "expected": "ExecutionContext is returned; the customizer throws a RuntimeException during customization, which propagates out of createExecutionContext().",
      "description": "Verify that if the ExecutionContextCustomizer throws an exception, createExecutionContext() does not swallow it and the exception bubbles up to the caller."
    }
  ]
}