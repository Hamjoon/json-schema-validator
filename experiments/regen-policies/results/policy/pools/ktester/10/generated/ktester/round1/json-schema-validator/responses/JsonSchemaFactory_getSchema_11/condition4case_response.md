{
  "group": "JsonSchemaFactory.getSchema(URI, JsonNode) - basic functionality",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "java.net.URI.create(\"http://example.com/schema\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new com.fasterxml.jackson.databind.ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        }
      ],
      "expected": "non‑null JsonSchema instance whose SchemaLocation string equals \"http://example.com/schema\" and whose root node matches the supplied JsonNode",
      "description": "Happy‑path test: a well‑formed URI and a simple JSON schema node are supplied. The method should create a JsonSchema wrapper without throwing and the internal location should reflect the given URI."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "java.net.URI.create(\"file:/tmp/my-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new com.fasterxml.jackson.databind.ObjectMapper().readTree(\"{\\\"$id\\\":\\\"my-schema\\\",\\\"type\\\":\\\"string\\\"}\")"
        }
      ],
      "expected": "non‑null JsonSchema instance whose SchemaLocation string equals \"file:/tmp/my-schema.json\" and contains the supplied JSON node",
      "description": "Validate that file‑scheme URIs are handled identically to HTTP URIs – the factory must not treat the scheme specially."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "java.net.URI.create(\"\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new com.fasterxml.jackson.databind.ObjectMapper().readTree(\"{\\\"type\\\":\\\"null\\\"}\")"
        }
      ],
      "expected": "non‑null JsonSchema instance whose SchemaLocation string equals \"\" (empty) and contains the supplied JSON node",
      "description": "Edge case where the URI string is empty. The factory should still create a SchemaLocation and not fail."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "null"
        },
        {
          "parameter": "jsonNode",
          "value": "new com.fasterxml.jackson.databind.ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
        }
      ],
      "expected": "java.lang.NullPointerException",
      "description": "When the URI argument is null the method should throw NPE because `schemaUri.toString()` is invoked."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "java.net.URI.create(\"http://example.com/invalid\")"
        },
        {
          "parameter": "jsonNode",
          "value": "null"
        }
      ],
      "expected": "java.lang.NullPointerException",
      "description": "When the JsonNode argument is null the method should throw NPE because the node is passed to `newJsonSchema` which expects a non‑null schema node."
    }
  ]
}