{
  "testCases": [
    {
      "group": "Valid inputs – custom configuration applied",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/custom-config"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setTypeLoose(true).setFailFast(true)"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"http://example.com/custom-config\", config.typeLoose == true, config.failFast == true)",
          "description": "When a valid URI, a proper JSON schema node and a non‑default configuration are supplied, getSchema should honour the configuration and still return a JsonSchema instance."
        }
      ]
    },
    {
      "group": "URI with fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema#myFragment"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"http://example.com/schema#myFragment\")",
          "description": "The factory must preserve URI fragments when creating the schema; the returned JsonSchema should have the same fragment in its identifier."
        }
      ]
    },
    {
      "group": "URI containing query parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema?version=1&debug=true"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"http://example.com/schema?version=1&debug=true\")",
          "description": "A URI that includes query parameters should be accepted and reflected in the schema identifier; a null config should fall back to the default."
        }
      ]
    },
    {
      "group": "Non‑object JsonNode – array schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/array-schema"
            },
            {
              "parameter": "jsonNode",
              "value": "[{\"type\":\"string\"},{\"type\":\"integer\"}]"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaNode is an array)",
          "description": "Even if the supplied JsonNode is an array (which is not a typical schema root), the method should still construct a JsonSchema object without throwing."
        }
      ]
    },
    {
      "group": "Malformed URI – illegal characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://exa mple.com/ bad"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A URI containing spaces or other illegal characters cannot be parsed correctly; the factory should throw an IllegalArgumentException when attempting to create the schema."
        }
      ]
    },
    {
      "group": "File scheme URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/test-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"file:/tmp/test-schema.json\")",
          "description": "The factory must handle non‑HTTP URIs such as the file scheme; the returned schema should retain the file URI as its identifier."
        }
      ]
    },
    {
      "group": "Meta‑schema URI (draft‑07) – special handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://json-schema.org/draft-07/schema#"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"http://json-schema.org/draft-07/schema#\")",
          "description": "When the URI points to a known meta‑schema, the factory should still create a JsonSchema object and keep the meta‑schema URI unchanged."
        }
      ]
    },
    {
      "group": "Relative URI resolved against base (using URI constructor)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "schemas/relative-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"rel\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"schemas/relative-schema.json\")",
          "description": "Even if the supplied URI is relative, the method should accept it and use the exact string as the schema identifier."
        }
      ]
    }
  ]
}