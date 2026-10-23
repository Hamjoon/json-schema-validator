{
  "testCases": [
    {
      "group": "JsonSchemaFactory.builder() – build() returns a factory",
      "cases": [
        {
          "input": [],
          "expected": "non‑null instance of JsonSchemaFactory",
          "description": "Invoke `JsonSchemaFactory.builder().build()` and verify that the returned object is a non‑null `JsonSchemaFactory`. This confirms that the builder can successfully create a factory instance."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.builder() – distinct factories on successive builds",
      "cases": [
        {
          "input": [],
          "expected": "two distinct JsonSchemaFactory instances on successive builds",
          "description": "Call `JsonSchemaFactory.builder().build()` twice and assert that the two returned `JsonSchemaFactory` objects are different (different references). This ensures that each `build()` call produces a fresh factory."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.builder() – default components of the built factory",
      "cases": [
        {
          "input": [],
          "expected": "non‑null URIFactory and URITranslator in the created factory",
          "description": "Create a factory via `JsonSchemaFactory.builder().build()` and then call `getUriFactory()` and `getUriTranslator()` on it. Both calls must return non‑null objects, confirming that the builder supplies default implementations for these required components."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.builder() – thread‑safety of builder creation",
      "cases": [
        {
          "input": [],
          "expected": "each thread receives a non‑null, distinct Builder instance",
          "description": "Spawn several threads (e.g., 10) that concurrently invoke `JsonSchemaFactory.builder()`. Collect the returned Builder objects and verify that none are null and that all references are unique. This checks that the static `builder()` method is safe to call from multiple threads without sharing state."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.builder() – builder does not retain state after build",
      "cases": [
        {
          "input": [],
          "expected": "second Builder after a build has default configuration",
          "description": "Obtain a Builder, call a configuration method on it (e.g., `setUriFactory(customFactory)` if available), then invoke `build()` to create a factory. Afterwards, obtain a new Builder via `JsonSchemaFactory.builder()` and verify that its configuration (e.g., `getUriFactory()`) reflects the defaults, not the custom settings applied to the previous builder. This confirms that the builder does not retain mutable state across separate invocations."
        }
      ]
    }
  ]
}