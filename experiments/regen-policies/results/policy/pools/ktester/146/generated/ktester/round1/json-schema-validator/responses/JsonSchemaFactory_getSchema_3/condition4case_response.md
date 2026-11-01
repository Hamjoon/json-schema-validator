{
  "group": "JsonSchemaFactory.getSchema(JsonNode) - basic scenarios",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema version matches the default (e.g., Draft‑7). The schema should contain a property named \"name\" of type string.",
      "description": "Pass a well‑formed JSON schema node. The method should create and return a JsonSchema object without throwing an exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{}"
        }
      ],
      "expected": "non‑null JsonSchema instance representing an empty schema (no constraints).",
      "description": "Pass an empty JSON object. The factory should still return a valid JsonSchema instance (an empty schema)."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "Pass a null JsonNode reference. The implementation forwards the argument to newJsonSchema which expects a non‑null node, so a NullPointerException should be thrown."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "[ { \"type\": \"string\" }, { \"type\": \"number\" } ]"
        }
      ],
      "expected": "non‑null JsonSchema instance representing an array schema (type \"array\") with items schema derived from the first element.",
      "description": "Pass a JsonNode that is a JSON array. The factory should still create a JsonSchema object; the exact internal representation depends on the library but the call must succeed."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"string\" }"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema is Draft‑07 (as indicated by the $schema keyword).",
      "description": "Provide a schema node that explicitly declares its meta‑schema via the $schema keyword. The factory should honour the declared version when constructing the JsonSchema."
    }
  ]
}