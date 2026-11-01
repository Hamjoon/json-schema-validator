{
  "group": "JsonSchema.getId",
  "cases": [
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema\""
        }
      ],
      "expected": "\"http://example.com/schema\"",
      "description": "Typical non‑empty URI identifier – getId() should return the exact value supplied to the constructor."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"\""
        }
      ],
      "expected": "\"\"",
      "description": "Empty string identifier – verifies that getId() correctly returns an empty string without throwing."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "null"
        }
      ],
      "expected": "null",
      "description": "Null identifier – ensures that when the JsonSchema is created with a null id, getId() returns null as specified."
    }
  ]
}