{
  "testCases": [
    {
      "group": "Valid inputs – schema is created successfully",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (schemaId == \"http://example.com/schema\")",
          "description": "When a well‑formed URI, a valid JSON schema node and a default configuration are supplied, getSchema should return a JsonSchema object representing the schema."
        }
      ]
    },
    {
      "group": "Null handling – URI is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": null
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
          "expected": "NullPointerException",
          "description": "The factory cannot create a schema without a base URI; passing null should cause a NullPointerException (as newJsonSchema dereferences the URI)."
        }
      ]
    },
    {
      "group": "Null handling – JsonNode is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema"
            },
            {
              "parameter": "jsonNode",
              "value": null
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null JsonNode means there is no schema definition; the method is expected to throw NullPointerException when it tries to inspect the node."
        }
      ]
    },
    {
      "group": "Null handling – Config is null (default config used)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "non‑null JsonSchema instance (default configuration applied)",
          "description": "When the configuration argument is null the factory should fall back to its default configuration and still return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Edge case – Empty JSON object as schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/empty"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema instance (represents an empty schema)",
          "description": "Even an empty JSON object is a syntactically valid schema; the method should still produce a JsonSchema object without throwing."
        }
      ]
    }
  ]
}