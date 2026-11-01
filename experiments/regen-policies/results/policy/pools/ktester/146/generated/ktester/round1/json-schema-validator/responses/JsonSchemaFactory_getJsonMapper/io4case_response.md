{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getJsonMapper() - null jsonMapper branch",
      "cases": [
        {
          "input": [],
          "expected": "the ObjectMapper instance returned by JsonMapperFactory.getInstance() (reference equality)",
          "description": "Create a JsonSchemaFactory instance without providing a custom ObjectMapper (e.g., via the default builder or by using reflection to instantiate the class). Because the private field `jsonMapper` is null, getJsonMapper() should take the false branch of the ternary operator and return exactly the singleton instance supplied by JsonMapperFactory.getInstance()."
        },
        {
          "input": [],
          "expected": "the same ObjectMapper instance on consecutive calls",
          "description": "Call getJsonMapper() twice on a factory whose `jsonMapper` field is null and verify that both calls return the identical ObjectMapper reference, confirming that the fallback does not create a new mapper on each invocation."
        }
      ]
    }
  ]
}