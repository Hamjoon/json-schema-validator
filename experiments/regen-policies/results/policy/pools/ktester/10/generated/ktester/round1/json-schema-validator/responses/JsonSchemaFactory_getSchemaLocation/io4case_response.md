{
  "group": "JsonSchemaFactory.getSchemaLocation",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaRetrievalUri",
          "value": "null"
        },
        {
          "parameter": "schemaNode",
          "value": "null"
        },
        {
          "parameter": "validationContext",
          "value": "mock ValidationContext where resolveSchemaId(null) returns null"
        }
      ],
      "expected": "SchemaLocation.DOCUMENT",
      "description": "Both `schemaNode` and `validationContext` return null, and no fallback URI is supplied – the method should return the constant `SchemaLocation.DOCUMENT`."
    },
    {
      "input": [
        {
          "parameter": "schemaRetrievalUri",
          "value": "null"
        },
        {
          "parameter": "schemaNode",
          "value": "any JsonNode (mocked)"
        },
        {
          "parameter": "validationContext",
          "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns \"\" (empty string)"
        }
      ],
      "expected": "SchemaLocation.of(\"\")",
      "description": "The ValidationContext resolves an empty string (which is not `null`). The method should treat it as a valid ID and return `SchemaLocation.of(\"\")`, ignoring the null `schemaRetrievalUri`."
    },
    {
      "input": [
        {
          "parameter": "schemaRetrievalUri",
          "value": "SchemaLocation.of(\"\")"
        },
        {
          "parameter": "schemaNode",
          "value": "any JsonNode (mocked)"
        },
        {
          "parameter": "validationContext",
          "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns null"
        }
      ],
      "expected": "SchemaLocation.of(\"\")",
      "description": "When the ValidationContext returns `null` and the supplied `schemaRetrievalUri` represents an empty string, the method should fall back to that empty string location."
    },
    {
      "input": [
        {
          "parameter": "schemaRetrievalUri",
          "value": "SchemaLocation.of(\"http://fallback.com/schema\")"
        },
        {
          "parameter": "schemaNode",
          "value": "any JsonNode (mocked)"
        },
        {
          "parameter": "validationContext",
          "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns \"\" (empty string)"
        }
      ],
      "expected": "SchemaLocation.of(\"\")",
      "description": "Even when a non‑null fallback URI is provided, a resolved empty string from the ValidationContext takes precedence, resulting in `SchemaLocation.of(\"\")`."
    },
    {
      "input": [
        {
          "parameter": "schemaRetrievalUri",
          "value": "null"
        },
        {
          "parameter": "schemaNode",
          "value": "any JsonNode (mocked)"
        },
        {
          "parameter": "validationContext",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "Passing a `null` ValidationContext should cause a `NullPointerException` because the method dereferences it to call `resolveSchemaId`."
    }
  ]
}