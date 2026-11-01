{
  "testCases": [
    {
      "group": "Valid inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"object\"}')"
            }
          ],
          "expected": "non‑null JsonSchema instance (schema is successfully created)",
          "description": "Both parameters are valid – a reachable schema location and a JSON node describing a simple object schema. The factory should return a JsonSchema instance without throwing."
        }
      ]
    },
    {
      "group": "Null schemaUri",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"string\"}')"
            }
          ],
          "expected": "NullPointerException",
          "description": "schemaUri is null while jsonNode is valid. The method should fail fast and throw NPE because a schema location is required to resolve meta‑schema and references."
        }
      ]
    },
    {
      "group": "Null jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "jsonNode is null while schemaUri is valid. The factory cannot build a schema without the JSON representation, so it should throw NPE."
        }
      ]
    },
    {
      "group": "Both parameters null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Both arguments are null. The method is expected to throw NPE (the first null encountered)."
        }
      ]
    },
    {
      "group": "Relative schema location",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"relative/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$id\":\"http://example.com/relative/schema.json\",\"type\":\"array\"}')"
            }
          ],
          "expected": "non‑null JsonSchema instance with base URI resolved to the relative location",
          "description": "A relative path is supplied. The factory should resolve it against the default base URI and still return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Non‑existent schema location",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/does-not-exist.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"null\"}')"
            }
          ],
          "expected": "RuntimeException (or SchemaLoadingException) indicating the resource cannot be found",
          "description": "The supplied schemaUri points to a resource that does not exist. The factory should propagate an exception indicating loading failure."
        }
      ]
    }
  ]
}