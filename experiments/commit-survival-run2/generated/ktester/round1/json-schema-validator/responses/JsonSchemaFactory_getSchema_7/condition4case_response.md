{
  "testCases": [
    {
      "group": "Valid inputs – both JsonNode and Config provided",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()" 
            }
          ],
          "expected": "JsonSchema instance whose internal schema node equals the supplied JsonNode and whose config equals the supplied config",
          "description": "When a well‑formed JsonNode and a non‑null SchemaValidatorsConfig are supplied, getSchema should create a JsonSchema wrapping the node without throwing any exception."
        }
      ]
    },
    {
      "group": "Valid inputs – config is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"string\" }"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance whose internal schema node equals the supplied JsonNode and whose config is null (or default config used internally)",
          "description": "The method should accept a null config and fall back to the factory’s default configuration rather than throwing an exception."
        }
      ]
    },
    {
      "group": "Invalid input – jsonNode is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) indicating that the schema JsonNode cannot be null",
          "description": "Passing a null JsonNode is illegal; the method (via newJsonSchema) should raise an exception rather than returning a JsonSchema."
        }
      ]
    },
    {
      "group": "Invalid input – both parameters null",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) because the schema JsonNode is required",
          "description": "Even when config is also null, the missing JsonNode should cause the method to fail fast."
        }
      ]
    },
    {
      "group": "Edge case – empty JSON object",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance that represents an empty schema (no validation rules)",
          "description": "An empty JsonNode should still produce a valid JsonSchema object; this verifies that the method does not treat empty objects as invalid."
        }
      ]
    },
    {
      "group": "Edge case – malformed JSON node (non‑object root)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "\"just a string\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance (factory may accept any JsonNode) or IllegalArgumentException if the implementation validates the root type",
          "description": "Testing how the method behaves when the supplied JsonNode is a primitive value rather than an object; some implementations allow it, others reject it."
        }
      ]
    }
  ]
}