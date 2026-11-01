{
  "testCases": [
    {
      "group": "Valid inputs",
      "cases": [
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "JsonSchema instance is returned; its schemaLocation equals \"http://example.com/schema.json\" and its schemaNode equals the supplied JsonNode",
          "description": "Happy‑path test: all arguments are non‑null and well‑formed; verifies that getSchema forwards the arguments to newJsonSchema and the resulting JsonSchema reflects them."
        },
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"integer\\\"}\")" },
            { "parameter": "config", "value": "null" }
          ],
          "expected": "JsonSchema instance is returned; its schemaLocation equals \"http://example.com/schema.json\" and its schemaNode equals the supplied JsonNode; a default SchemaValidatorsConfig is used internally",
          "description": "Pass a null config to ensure the factory creates a default configuration rather than throwing an exception."
        },
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "null" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "JsonSchema instance is returned; its schemaLocation equals \"http://example.com/schema.json\" and its schemaNode is null",
          "description": "Pass a null JsonNode to verify that the method does not reject a missing schema document (some callers use a separate schema loader)."
        }
      ]
    },
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            { "parameter": "schemaUri", "value": "null" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "NullPointerException is thrown",
          "description": "A null URI should cause an NPE because the method dereferences schemaUri.toString()."
        },
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "null" },
            { "parameter": "config", "value": "null" }
          ],
          "expected": "JsonSchema instance is returned; both jsonNode and config are null inside the created schema",
          "description": "Both optional arguments are null – the method should still succeed, delegating to newJsonSchema."
        }
      ]
    },
    {
      "group": "Edge URI cases",
      "cases": [
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json#fragment\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "JsonSchema instance is returned; its schemaLocation string includes the fragment \"#fragment\"",
          "description": "URI contains a fragment; verify that the fragment is preserved in the SchemaLocation used by the created JsonSchema."
        },
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json?version=1\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"null\\\"}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "JsonSchema instance is returned; its schemaLocation string includes the query part \"?version=1\"",
          "description": "URI contains a query string; ensure the full URI string is stored unchanged."
        }
      ]
    },
    {
      "group": "Config variations",
      "cases": [
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\", \\\"properties\\\":{}}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig(){ { setFailFast(true); } }" }
          ],
          "expected": "JsonSchema instance is returned; its internal validation context reflects the fail‑fast setting (e.g., first validation error aborts processing)",
          "description": "Pass a custom SchemaValidatorsConfig with failFast enabled and verify that the resulting JsonSchema is created with that configuration."
        },
        {
          "input": [
            { "parameter": "schemaUri", "value": "new URI(\"http://example.com/schema.json\")" },
            { "parameter": "jsonNode", "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\", \\\"items\\\":{\\\"type\\\":\\\"string\\\"}}\")" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); } }" }
          ],
          "expected": "JsonSchema instance is returned; its validation context has typeLoose set to true",
          "description": "Configure the factory to allow loose type checking and ensure the flag propagates to the created JsonSchema."
        }
      ]
    }
  ]
}