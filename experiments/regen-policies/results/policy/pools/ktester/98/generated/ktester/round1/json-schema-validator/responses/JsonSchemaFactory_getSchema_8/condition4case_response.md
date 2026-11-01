{
  "group": "JsonSchemaFactory.getSchema(JsonNode, SchemaValidatorsConfig)",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "Returns a non‑null JsonSchema instance whose meta‑schema matches the default meta‑schema and contains the supplied schema node.",
      "description": "Typical usage: both JsonNode and SchemaValidatorsConfig are provided. The method should delegate to newJsonSchema and produce a valid JsonSchema object."
    },
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
      "expected": "Throws NullPointerException (or IllegalArgumentException) because the schema JsonNode is required.",
      "description": "Edge case where the schema node is null. The factory should reject a null JsonNode and raise an exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "Returns a non‑null JsonSchema instance using the default SchemaValidatorsConfig.",
      "description": "Config parameter is optional; when null the factory should fall back to its default configuration."
    },
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
      "expected": "Throws NullPointerException (or IllegalArgumentException) because the schema JsonNode is required, regardless of config being null.",
      "description": "Both parameters are null – the method must still reject the missing JsonNode and raise an exception."
    }
  ]
}