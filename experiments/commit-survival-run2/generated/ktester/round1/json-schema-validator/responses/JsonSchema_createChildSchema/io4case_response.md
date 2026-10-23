{
  "testCases": [
    {
      "group": "Empty schemaPath",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"\""
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectNode representing {\"type\":\"string\"}"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. The ValidationContext receives an empty string as the path. No exception is thrown.",
          "description": "Ensures that an empty string for schemaPath is accepted and forwarded unchanged to ValidationContext.newSchema."
        }
      ]
    },
    {
      "group": "Whitespace schemaPath",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"   \""
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectNode representing {\"type\":\"integer\"}"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned. The ValidationContext receives the whitespace string unchanged. No exception is thrown.",
          "description": "Verifies that a schemaPath consisting solely of whitespace characters does not cause errors and is passed through as‑is."
        }
      ]
    },
    {
      "group": "ArrayNode schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/items\""
            },
            {
              "parameter": "schemaNode",
              "value": "ArrayNode representing [{\"type\":\"string\"}, {\"type\":\"number\"}]"
            }
          ],
          "expected": "A non‑null JsonSchema child schema is returned. The ArrayNode is passed to ValidationContext unchanged. No exception is thrown.",
          "description": "Checks that createChildSchema accepts a JsonNode that is an ArrayNode (i.e., not an ObjectNode) and forwards it without validation of its shape."
        }
      ]
    },
    {
      "group": "TextNode schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/description\""
            },
            {
              "parameter": "schemaNode",
              "value": "TextNode representing \"just a string\""
            }
          ],
          "expected": "A non‑null JsonSchema child schema is returned. The TextNode is forwarded to ValidationContext. No exception is thrown.",
          "description": "Validates that a TextNode (a primitive JSON value) can be supplied as schemaNode and is not rejected by createChildSchema."
        }
      ]
    },
    {
      "group": "BooleanNode schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/flag\""
            },
            {
              "parameter": "schemaNode",
              "value": "BooleanNode representing true"
            }
          ],
          "expected": "A non‑null JsonSchema child schema is returned. The BooleanNode is passed to ValidationContext unchanged. No exception is thrown.",
          "description": "Ensures that a BooleanNode can be used as the schema definition argument without causing a NullPointerException or other error."
        }
      ]
    },
    {
      "group": "Complex nested schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/definitions/person\""
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectNode representing {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"},\"address\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}}}"
            }
          ],
          "expected": "A non‑null JsonSchema child schema is returned. The complex ObjectNode is forwarded to ValidationContext. No exception is thrown.",
          "description": "Tests that a deeply nested schema definition is accepted and correctly handed off to the ValidationContext."
        }
      ]
    },
    {
      "group": "ValidationContext throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/invalid\""
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectNode representing {\"type\":\"unknown\"}"
            }
          ],
          "expected": "RuntimeException (or subclass) thrown by ValidationContext.newSchema propagates out of createChildSchema. The test asserts that the same exception type and message are observed.",
          "description": "Simulates a mis‑behaving ValidationContext that throws an exception while creating a child schema, verifying that createChildSchema does not swallow the exception."
        }
      ]
    },
    {
      "group": "ValidationContext returns same instance as parent",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/loop\""
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectNode representing {\"$ref\":\"#\"}"
            }
          ],
          "expected": "The JsonSchema instance returned by createChildSchema is exactly the same object as the parent schema (as supplied by the mocked ValidationContext). No exception is thrown.",
          "description": "Ensures that createChildSchema returns whatever ValidationContext.newSchema returns, even if it is the parent instance itself, confirming no additional wrapping or cloning occurs."
        }
      ]
    }
  ]
}