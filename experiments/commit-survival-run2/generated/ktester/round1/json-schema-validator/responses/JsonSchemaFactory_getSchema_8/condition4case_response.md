{
  "testCases": [
    {
      "group": "Valid inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "non‑null JsonSchema instance (schema loaded without errors)",
          "description": "When a well‑formed URI and a valid JSON schema node are supplied, getSchema should return a JsonSchema object representing the supplied schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/local-schema.yaml"
            },
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"
            }
          ],
          "expected": "non‑null JsonSchema instance",
          "description": "Supports different URI schemes (e.g., file) and YAML‑compatible JSON nodes; the method should still return a JsonSchema instance."
        }
      ]
    },
    {
      "group": "Null URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": null
            },
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\" }"
            }
          ],
          "expected": "NullPointerException or IllegalArgumentException",
          "description": "If the schemaUri argument is null, the factory should reject the call and throw an exception rather than returning a JsonSchema."
        }
      ]
    },
    {
      "group": "Null JsonNode handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/any-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": null
            }
          ],
          "expected": "NullPointerException or IllegalArgumentException",
          "description": "When the jsonNode argument is null, getSchema must fail fast with an appropriate exception."
        }
      ]
    },
    {
      "group": "Both arguments null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": null
            },
            {
              "parameter": "jsonNode",
              "value": null
            }
          ],
          "expected": "NullPointerException or IllegalArgumentException",
          "description": "Both parameters null – the method should still throw an exception, confirming defensive null‑checking."
        }
      ]
    },
    {
      "group": "Invalid URI format",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "ht!tp://[invalid]"
            },
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\" }"
            }
          ],
          "expected": "URISyntaxException wrapped in IllegalArgumentException",
          "description": "A malformed URI string should cause the factory to reject the request, typically via an IllegalArgumentException (or a wrapped URISyntaxException)."
        }
      ]
    },
    {
      "group": "Empty JSON node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/empty-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            }
          ],
          "expected": "non‑null JsonSchema instance (may represent an empty schema)",
          "description": "Even an empty JSON object is a valid schema node; the method should still construct a JsonSchema instance without throwing."
        }
      ]
    }
  ]
}