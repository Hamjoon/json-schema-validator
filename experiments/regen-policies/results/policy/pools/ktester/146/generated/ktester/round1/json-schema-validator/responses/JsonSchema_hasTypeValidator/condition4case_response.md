{
  "group": "hasTypeValidator",
  "cases": [
    {
      "input": [],
      "expected": true,
      "description": "When the JsonSchema instance has a non‑null TypeValidator (e.g., created from a schema that defines a \"type\" keyword), hasTypeValidator() should return true."
    },
    {
      "input": [],
      "expected": false,
      "description": "When the JsonSchema instance has no TypeValidator (e.g., created from a schema without a \"type\" keyword), hasTypeValidator() should return false."
    },
    {
      "input": [],
      "expected": "No exception",
      "description": "Calling hasTypeValidator() should never throw an exception regardless of the internal state of the schema."
    }
  ]
}