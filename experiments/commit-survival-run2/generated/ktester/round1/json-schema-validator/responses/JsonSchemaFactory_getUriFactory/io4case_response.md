{
  "testCases": [
    {
      "group": "Custom URI Factory",
      "cases": [
        {
          "input": [],
          "expected": "Non‑null instance of the custom URIFactory supplied to the builder, and it is the exact same object (reference equality).",
          "description": "Create a custom implementation of `URIFactory` (e.g., a stub that always returns a fixed URI). Build a `JsonSchemaFactory` with `JsonSchemaFactory.builder().uriFactory(customFactory).build()`. Verify that `getUriFactory()` returns a non‑null object and that `factory.getUriFactory() == customFactory`."
        }
      ]
    },
    {
      "group": "Blueprint Inheritance",
      "cases": [
        {
          "input": [],
          "expected": "The returned URIFactory is the same instance that was present in the blueprint factory.",
          "description": "Create a blueprint factory using a custom `URIFactory` as in the previous case. Then build a new factory with `JsonSchemaFactory.builder(blueprint).build()`. Assert that the new factory’s `getUriFactory()` reference equals the blueprint’s `getUriFactory()` reference."
        },
        {
          "input": [],
          "expected": "The returned URIFactory is the default implementation when the blueprint is created with the default builder.",
          "description": "Create a blueprint factory using the default builder (`JsonSchemaFactory.builder().build()`). Build a second factory from this blueprint (`JsonSchemaFactory.builder(blueprint).build()`). Verify that both factories expose the same default `URIFactory` instance."
        }
      ]
    },
    {
      "group": "Multiple Calls Consistency",
      "cases": [
        {
          "input": [],
          "expected": "Identical object reference on successive calls.",
          "description": "For any `JsonSchemaFactory` instance (default, custom, or blueprint‑derived), invoke `getUriFactory()` multiple times and assert that each call returns the exact same object (`factory.getUriFactory() == factory.getUriFactory()`). This ensures the method does not create a new instance on each invocation."
        }
      ]
    },
    {
      "group": "Null Builder Argument Safety",
      "cases": [
        {
          "input": [],
          "expected": "Throws NullPointerException when `builder(null)` is invoked.",
          "description": "Attempt to create a factory with `JsonSchemaFactory.builder(null)`. The expected behavior is that the method validates its argument and throws a `NullPointerException`. This test confirms that the factory does not silently fallback to a default URI factory when a null blueprint is supplied."
        }
      ]
    }
  ]
}