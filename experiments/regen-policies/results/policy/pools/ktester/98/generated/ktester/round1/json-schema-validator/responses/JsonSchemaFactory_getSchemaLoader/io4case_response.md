{
  "tests": [
    {
      "group": "JsonSchemaFactory.getSchemaLoader - distinct factories",
      "cases": [
        {
          "input": [],
          "expected": "Two factories created via `JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)` must have different `SchemaLoader` objects (loader1 != loader2)",
          "description": "Ensures that the factory does not use a global singleton `SchemaLoader`; each factory instance should own its own loader."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchemaLoader - after other operations",
      "cases": [
        {
          "input": [],
          "expected": "The `SchemaLoader` returned before and after calling any `getSchema(...)` overload remains the identical object (loaderBefore == loaderAfter)",
          "description": "Verifies that invoking other public methods of the factory does not replace or re‑initialize the internal `schemaLoader`."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchemaLoader - custom subclass preservation",
      "cases": [
        {
          "input": [],
          "expected": "When a custom `SchemaLoader` subclass (e.g., `MySchemaLoader`) is supplied through the builder, `getSchemaLoader()` returns an instance of that exact subclass",
          "description": "Checks that the getter does not cast or wrap the supplied loader and that the runtime type is preserved."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchemaLoader - concurrent access",
      "cases": [
        {
          "input": [],
          "expected": "Concurrent invocations of `getSchemaLoader()` from multiple threads all return the same reference (all == the loader stored in the factory)",
          "description": "Tests thread‑safety of the accessor; even under parallel calls the factory must not create additional loader instances."
        }
      ]
    }
  ]
}