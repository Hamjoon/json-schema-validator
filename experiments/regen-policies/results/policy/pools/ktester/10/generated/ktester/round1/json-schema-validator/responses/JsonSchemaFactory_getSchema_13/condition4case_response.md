{
  "testCases": [
    {
      "group": "Valid inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "instance of JsonSchema (non‑null)",
          "description": "When a well‑formed URI, a valid JSON schema node and a non‑null configuration are supplied, getSchema should return a JsonSchema object representing the supplied schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/tmp/my-schema.yaml\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"string\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "instance of JsonSchema (non‑null)",
          "description": "When the configuration argument is null the factory should fall back to its default configuration and still produce a JsonSchema instance."
        }
      ]
    },
    {
      "group": "Null handling / exceptional paths",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null URI should cause a NullPointerException because the method dereferences schemaUri.toString()."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/invalid\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Supplying a null JsonNode (the schema definition) is not allowed and should result in an IllegalArgumentException (or NullPointerException depending on implementation)."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"null\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "An empty string URI is syntactically valid but the factory treats it as an illegal schema location and should throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Edge cases for schema location handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json#fragment\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"integer\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "instance of JsonSchema (non‑null) with proper fragment handling",
          "description": "When the URI contains a fragment, the factory must correctly create a SchemaLocation that includes the fragment; the returned JsonSchema should be created without error."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json?query=1\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "instance of JsonSchema (non‑null)",
          "description": "URIs with query components should be accepted; the method should still return a valid JsonSchema object."
        }
      ]
    }
  ]
}