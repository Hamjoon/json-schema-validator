{
  "tests": [
    {
      "group": "yamlMapper present (non‑null field)",
      "cases": [
        {
          "input": [],
          "expected": "the same ObjectMapper instance that was injected into the JsonSchemaFactory",
          "description": "When the private final field `yamlMapper` is initialized (non‑null), `getYamlMapper()` must return that exact instance rather than delegating to `YamlMapperFactory`. The test creates a JsonSchemaFactory (via reflection or a builder) with a mock ObjectMapper, invokes `getYamlMapper()` and asserts reference equality with the injected mapper."
        }
      ]
    },
    {
      "group": "yamlMapper absent (null field)",
      "cases": [
        {
          "input": [],
          "expected": "the instance returned by YamlMapperFactory.getInstance()",
          "description": "When the `yamlMapper` field is null, `getYamlMapper()` should fall back to the singleton provided by `YamlMapperFactory`. The test constructs a JsonSchemaFactory whose `yamlMapper` field is forced to null (reflection), calls `getYamlMapper()`, and verifies that the returned mapper is exactly the same object as `YamlMapperFactory.getInstance()`."
        },
        {
          "input": [],
          "expected": "subsequent calls return the same YamlMapperFactory instance (no new objects created)",
          "description": "Ensures the fallback path is deterministic: after the first call returns the factory singleton, a second call to `getYamlMapper()` still returns the same singleton instance, confirming no hidden state changes."
        }
      ]
    }
  ]
}