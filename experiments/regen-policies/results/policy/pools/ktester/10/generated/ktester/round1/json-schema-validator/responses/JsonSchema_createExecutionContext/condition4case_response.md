{
  "group": "createExecutionContext",
  "cases": [
    {
      "input": [],
      "expected": "ExecutionContext is returned whose ExecutionConfig.locale equals the locale from the SchemaValidatorsConfig, ExecutionConfig.formatAssertionsEnabled equals the flag from the config, and no customizer is invoked (i.e., the ExecutionContext remains unmodified).",
      "description": "When the ValidationContext's config does not provide an ExecutionContextCustomizer (null), the method should create a fresh ExecutionContext populated with the config's locale and format‑assertions flag and return it without any further modification."
    },
    {
      "input": [],
      "expected": "ExecutionContext is returned with the same locale and formatAssertionsEnabled values as in the config, and additionally the provided ExecutionContextCustomizer has been invoked – e.g., it sets a custom flag or property on the ExecutionContext that can be asserted after the call.",
      "description": "When the ValidationContext's config supplies a non‑null ExecutionContextCustomizer, the method must invoke the customizer's `customize` method passing the newly created ExecutionContext and the ValidationContext. The test verifies that the customizer was called and that its side‑effects are reflected in the returned ExecutionContext."
    }
  ]
}