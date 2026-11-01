{
  "testCases": [
    {
      "group": "multiple calls with null jsonMapper",
      "cases": [
        {
          "input": [],
          "expected": "The same ObjectMapper instance is returned on successive calls and JsonMapperFactory.getInstance() is invoked only once.",
          "description": "Create a JsonSchemaFactory without supplying a custom ObjectMapper (jsonMapper field is null). Call getJsonMapper() twice and capture the returned instances. Assert that both references are identical and use a Mockito spy on JsonMapperFactory to verify that getInstance() was called exactly once, confirming that the method does not repeatedly request the default mapper."
        }
      ]
    },
    {
      "group": "builder with custom ObjectMapper",
      "cases": [
        {
          "input": [],
          "expected": "Returns the custom ObjectMapper that was supplied to the builder.",
          "description": "Use JsonSchemaFactory.builder() (or the overload that accepts a blueprint) and invoke a hypothetical builder method setObjectMapper(customMapper) before building the factory. Call getJsonMapper() and assert that the returned mapper is the same reference as customMapper, exercising the true branch of the ternary operator via the public builder API."
        }
      ]
    },
    {
      "group": "builder with explicit null ObjectMapper",
      "cases": [
        {
          "input": [],
          "expected": "Falls back to JsonMapperFactory.getInstance() when the builder is instructed to use a null mapper.",
          "description": "Construct a JsonSchemaFactory via its builder, explicitly passing null to the mapper configuration (e.g., .setObjectMapper(null)). After building, invoke getJsonMapper() and verify that the result equals JsonMapperFactory.getInstance(), thereby covering the false branch when the field is intentionally set to null."
        }
      ]
    },
    {
      "group": "concurrent invocations with null jsonMapper",
      "cases": [
        {
          "input": [],
          "expected": "All threads receive the identical default ObjectMapper instance and JsonMapperFactory.getInstance() is called only once.",
          "description": "Instantiate a JsonSchemaFactory with no custom mapper. Spawn a pool of several threads (e.g., 10) where each thread calls getJsonMapper() and stores the result in a thread‑safe collection. After all threads complete, assert that every stored reference is the same object and verify via a spy that JsonMapperFactory.getInstance() was invoked a single time, proving that the method behaves correctly under concurrent access."
        }
      ]
    },
    {
      "group": "JsonMapperFactory returns null",
      "cases": [
        {
          "input": [],
          "expected": "getJsonMapper() returns null when JsonMapperFactory.getInstance() returns null.",
          "description": "Mock (or spy) JsonMapperFactory so that its static getInstance() method returns null. Create a JsonSchemaFactory with a null jsonMapper field and invoke getJsonMapper(). The method should propagate the null result, allowing verification that no NullPointerException is thrown and that the fallback path simply returns whatever the factory supplies."
        }
      ]
    }
  ]
}