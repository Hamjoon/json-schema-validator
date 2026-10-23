{
  "testCases": [
    {
      "group": "Boolean Schema Node (true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"true\")"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose getSchemaNode() is a BooleanNode with value true",
          "description": "A JSON‑Schema can be the boolean literal true, meaning any instance is valid. The factory must accept a BooleanNode and return a JsonSchema wrapper without throwing."
        }
      ]
    },
    {
      "group": "Boolean Schema Node (false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"false\")"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose getSchemaNode() is a BooleanNode with value false",
          "description": "A JSON‑Schema can be the boolean literal false, meaning no instance is valid. The factory must accept a BooleanNode and produce a JsonSchema object."
        }
      ]
    },
    {
      "group": "Root Node as Number",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"123\")"
            }
          ],
          "expected": "JsonSchemaValidationException (or any RuntimeException) because a numeric node is not a valid schema root",
          "description": "Passing a numeric JsonNode as the schema root should be rejected during schema creation, resulting in an exception."
        }
      ]
    },
    {
      "group": "Root Node as Text",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('\"just a string\"')"
            }
          ],
          "expected": "JsonSchemaValidationException (or any RuntimeException) because a text node is not a valid schema root",
          "description": "A plain string node cannot represent a JSON‑Schema; the factory should throw an exception."
        }
      ]
    },
    {
      "group": "Unsupported $schema URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$schema\":\"http://example.com/unknown-schema#\",\"type\":\"string\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that falls back to the factory's default meta‑schema version",
          "description": "When the $schema keyword points to an unknown or unsupported meta‑schema, the factory should not fail but should use the default version (e.g., Draft‑2020‑12) to create the schema."
        }
      ]
    },
    {
      "group": "$schema with whitespace and fragment variations",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$schema\":\" http://json-schema.org/draft-07/schema# \",\"type\":\"integer\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that correctly normalises the URI and uses Draft‑07 meta‑schema",
          "description": "The factory should trim whitespace and handle trailing fragments when resolving the $schema URI."
        }
      ]
    },
    {
      "group": "Schema with $id and relative $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$id\":\"http://example.com/root.json\",\"definitions\":{\"myString\":{\"type\":\"string\"}},\"$ref\":\"#/definitions/myString\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where internal resolution of $ref succeeds (no exception during creation)",
          "description": "A schema that defines an $id and uses a relative $ref should be parsed successfully; the factory must resolve the reference during schema construction."
        }
      ]
    },
    {
      "group": "Schema with external $ref (URI only)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$ref\":\"http://example.com/external-schema.json\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) – creation succeeds; external resolution is deferred to validation time",
          "description": "When the root schema contains only an external $ref, getSchema should still create a JsonSchema object without trying to fetch the external document at construction."
        }
      ]
    },
    {
      "group": "Complex Nested Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"type\":\"object\",\"properties\":{\"address\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"}}},\"age\":{\"type\":\"integer\",\"minimum\":0}}}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose getSchemaNode() equals the supplied complex node",
          "description": "A realistic, multi‑level schema should be accepted; the factory must correctly preserve the whole node hierarchy."
        }
      ]
    },
    {
      "group": "Schema with URN $schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$schema\":\"urn:json-schema:org:json-schema:draft-07\",\"type\":\"boolean\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that resolves the URN to Draft‑07 meta‑schema",
          "description": "The factory supports URN‑based meta‑schema identifiers; this test verifies that a URN $schema is correctly translated to the appropriate version."
        }
      ]
    },
    {
      "group": "Schema with duplicate keywords (overridden)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"type\":\"object\",\"type\":\"string\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where the last occurrence of the keyword wins (type = \"string\")",
          "description": "When a schema node contains duplicate keywords, Jackson retains the last one. The factory should not fail and should use the resulting node."
        }
      ]
    }
  ]
}