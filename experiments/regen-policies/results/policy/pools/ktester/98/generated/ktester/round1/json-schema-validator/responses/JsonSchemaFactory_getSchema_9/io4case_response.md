{
  "testCases": [
    {
      "group": "Boolean schema node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"true\")"
            }
          ],
          "expected": "non‑null JsonSchema instance (the boolean node is accepted as a valid schema per the JSON‑Schema spec)",
          "description": "The JSON node is a literal boolean `true`, which is a legal JSON‑Schema (always‑valid). The factory should create a JsonSchema without throwing, regardless of the schema location."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"false\")"
            }
          ],
          "expected": "non‑null JsonSchema instance (the boolean node is accepted as a valid schema per the JSON‑Schema spec)",
          "description": "The JSON node is a literal boolean `false`, also a legal schema (always‑invalid). The method should succeed and return a JsonSchema."
        }
      ]
    },
    {
      "group": "Schema node with unknown keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"object\",\"unknownKeyword\":123}')"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating an unrecognized keyword",
          "description": "The schema contains a keyword that is not defined in any supported meta‑schema. The factory should fail during schema compilation and throw an appropriate exception."
        }
      ]
    },
    {
      "group": "SchemaUri with unsupported scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"string\"}')"
            }
          ],
          "expected": "SchemaLoadingException (or RuntimeException) indicating that the scheme `ftp` cannot be handled",
          "description": "The location uses a scheme that the factory does not know how to resolve. The method should raise an exception rather than silently ignore the scheme."
        }
      ]
    },
    {
      "group": "SchemaUri with empty string",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"number\"}')"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) because the URI string is empty/invalid",
          "description": "An empty string does not constitute a valid schema location. The factory should reject it early."
        }
      ]
    },
    {
      "group": "Valid location but mismatched jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}')"
            }
          ],
          "expected": "non‑null JsonSchema instance reflecting the provided jsonNode (the content of the location is ignored)",
          "description": "The schema location points to a valid file, but the caller supplies a different JSON node. The method should build the schema from the supplied node, not from the resource at the location."
        }
      ]
    },
    {
      "group": "JsonNode with $ref to missing external schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$ref\":\"nonexistent.json\"}')"
            }
          ],
          "expected": "SchemaLoadingException (or JsonSchemaException) indicating that the referenced schema cannot be found",
          "description": "The supplied schema node contains a `$ref` that points to a resource that does not exist. During schema creation the factory should attempt to resolve the reference and fail with an appropriate exception."
        }
      ]
    },
    {
      "group": "JsonNode with non‑object/boolean root (number)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/valid-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('123')"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) because a numeric root is not a valid JSON‑Schema",
          "description": "According to the spec, a schema must be either an object or a boolean. Supplying a numeric node should cause the factory to reject the input."
        }
      ]
    }
  ]
}