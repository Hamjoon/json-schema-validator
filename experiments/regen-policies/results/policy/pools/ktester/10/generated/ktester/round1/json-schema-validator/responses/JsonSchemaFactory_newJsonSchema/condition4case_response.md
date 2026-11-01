{
  "group": "newJsonSchema - initializeValidators handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "mock(SchemaLocation.class)"
        },
        {
          "parameter": "schemaNode",
          "value": "mock(JsonNode.class)"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "JsonSchema instance returned; jsonSchema.initializeValidators() invoked exactly once without throwing; no exception propagated.",
      "description": "Happy‑path: the created JsonSchema successfully initializes its validators. The try block executes without exception, so the method returns the JsonSchema produced by doCreate."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "mock(SchemaLocation.class)"
        },
        {
          "parameter": "schemaNode",
          "value": "mock(JsonNode.class)"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "JsonSchema instance returned; jsonSchema.initializeValidators() invoked and throws RuntimeException which is caught and ignored; method still returns the JsonSchema.",
      "description": "Exception‑path: simulate a failure while pre‑loading $ref validators. The exception is caught inside the method, so it should not propagate and the returned JsonSchema should still be non‑null."
    }
  ]
}