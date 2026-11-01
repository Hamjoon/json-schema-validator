{
  "group": "JsonSchemaFactory.getSchemaLoader",
  "cases": [
    {
      "input": [],
      "expected": "a non‑null SchemaLoader instance that is the same object stored inside the factory",
      "description": "When a factory is created via the default `JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)` call, `getSchemaLoader()` should return the internal `schemaLoader` field. The returned reference must be non‑null and identical (==) to the loader used by the factory."
    },
    {
      "input": [],
      "expected": "the custom SchemaLoader instance supplied through the Builder",
      "description": "Using `JsonSchemaFactory.builder()` a custom `SchemaLoader` (e.g., a mock or a `DefaultSchemaLoader` with a stubbed resolver) is injected via the builder’s `schemaLoader` method. After building the factory, `getSchemaLoader()` must return exactly that custom instance, confirming the getter does not create a new object."
    },
    {
      "input": [],
      "expected": "the same SchemaLoader instance on successive calls",
      "description": "For any created factory (default or custom), invoking `getSchemaLoader()` multiple times must return the identical object each time, proving the method is a simple accessor without side‑effects."
    }
  ]
}