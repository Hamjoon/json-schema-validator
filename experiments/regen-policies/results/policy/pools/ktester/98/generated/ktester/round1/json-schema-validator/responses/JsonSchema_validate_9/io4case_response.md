{
  "testCases": [
    {
      "group": "Missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({})"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"ERROR: age: is required\"",
          "description": "The schema defines the property \"age\" as required. An empty object should trigger a validation error indicating the missing required property."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({})"
            },
            {
              "parameter": "format",
              "value": "new CollectingListFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "[\"age: is required\"]",
          "description": "Using a List‑collecting OutputFormat, the same missing‑property case should produce a list containing a single ValidationMessage about the required field."
        }
      ]
    },
    {
      "group": "Disallowed additional property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":30,\"extra\":\"value\"})"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"ERROR: #: extraneous property \\\"extra\\\" is not allowed\"",
          "description": "The schema has \"additionalProperties\": false. Supplying an unexpected property should cause a validation error that mentions the extraneous property."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":30,\"extra\":\"value\"})"
            },
            {
              "parameter": "format",
              "value": "new CollectingListFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "[\"#: extraneous property \\\"extra\\\" is not allowed\"]",
          "description": "CollectingListFormat should return a list with the same additional‑property error message."
        }
      ]
    },
    {
      "group": "Array element type validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "arrayNode([1,2,\"three\"])"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"ERROR: [2]: integer expected\"",
          "description": "The schema expects an array of integers. The third element is a string, so validation must report an error at index 2."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "arrayNode([1,2,\"three\"])"
            },
            {
              "parameter": "format",
              "value": "new CollectingListFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "[\"[2]: integer expected\"]",
          "description": "CollectingListFormat should return a list containing the array‑element type error."
        }
      ]
    },
    {
      "group": "Root node type mismatch (primitive vs object)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "textNode(\"just a string\")"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"ERROR: #: object expected\"",
          "description": "The schema describes an object, but the supplied root node is a JSON string. Validation must fail with a type‑mismatch message."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "textNode(\"just a string\")"
            },
            {
              "parameter": "format",
              "value": "new CollectingListFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "[\"#: object expected\"]",
          "description": "List‑collecting format should contain the same type‑mismatch ValidationMessage."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer that disables fail‑fast",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":\"thirty\",\"name\":123})"
            },
            {
              "parameter": "format",
              "value": "new CollectingListFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(false))"
            }
          ],
          "expected": "[\"age: integer expected\",\"name: string expected\"]",
          "description": "With fail‑fast disabled the validator should collect **all** errors (type error on \"age\" and type error on \"name\"). The customizer explicitly sets failFast to false."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":\"thirty\",\"name\":123})"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(false))"
            }
          ],
          "expected": "\"ERROR: age: integer expected; name: string expected\"",
          "description": "SimpleStringFormat should concatenate all collected messages when fail‑fast is turned off."
        }
      ]
    }
  ]
}