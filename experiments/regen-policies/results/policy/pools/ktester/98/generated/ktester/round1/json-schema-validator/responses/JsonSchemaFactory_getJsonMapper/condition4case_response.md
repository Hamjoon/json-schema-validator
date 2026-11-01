{
  "testCases": [
    {
      "group": "jsonMapper field is non‑null",
      "cases": [
        {
          "input": [],
          "expected": "Returns the ObjectMapper instance that was injected into the JsonSchemaFactory (the same reference as the jsonMapper field).",
          "description": "Create a JsonSchemaFactory instance (via its Builder or a subclass in the same package) and set its private jsonMapper field to a mock or real ObjectMapper. Invoke getJsonMapper() and verify that the returned mapper is exactly the one that was set, confirming the true branch of the ternary operator."
        }
      ]
    },
    {
      "group": "jsonMapper field is null",
      "cases": [
        {
          "input": [],
          "expected": "Returns JsonMapperFactory.getInstance() (the default shared ObjectMapper).",
          "description": "Instantiate a JsonSchemaFactory without providing a custom ObjectMapper, leaving the jsonMapper field null. Call getJsonMapper() and assert that the result equals JsonMapperFactory.getInstance(). This covers the false branch of the ternary operator."
        },
        {
          "input": [],
          "expected": "JsonMapperFactory.getInstance() is invoked exactly once.",
          "description": "Using a spying technique (e.g., Mockito.spy on JsonMapperFactory), verify that getJsonMapper() triggers a single call to JsonMapperFactory.getInstance() when the internal jsonMapper field is null."
        }
      ]
    }
  ]
}