{
  "testCases": [
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
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
          "expected": "NullPointerException",
          "description": "When the JSON document (rootNode) is null the method should throw a NullPointerException because validation cannot be performed on a missing node."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"name\":\"John\"})"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the OutputFormat argument is null the method must reject the call with an IllegalArgumentException (the format is required to produce a result)."
        }
      ]
    },
    {
      "group": "Successful validation without customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":30})"
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
          "expected": "\"OK\"",
          "description": "Validate a JSON document that conforms to the schema (e.g., an integer \"age\" property). No customizer is supplied, so the default execution context is used. The SimpleStringFormat should return the string \"OK\" when there are no validation messages."
        }
      ]
    },
    {
      "group": "Validation with errors (no customizer)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":\"thirty\"})"
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
          "expected": "\"ERROR: age: integer expected\"",
          "description": "Validate a JSON document that violates the schema (string instead of integer for \"age\"). The format should receive the validation messages and return a string that contains the error description."
        }
      ]
    },
    {
      "group": "Validation with ExecutionContextCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":30})"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true))"
            }
          ],
          "expected": "\"OK\"",
          "description": "Provide a customizer that switches the execution context to fail‑fast mode. Because the document is valid, validation succeeds and the format returns \"OK\"."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":\"thirty\"})"
            },
            {
              "parameter": "format",
              "value": "new SimpleStringFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true))"
            }
          ],
          "expected": "\"ERROR: age: integer expected\"",
          "description": "Same fail‑fast customizer but the document contains a validation error. The validator should stop after the first error and the format should return the single error message."
        }
      ]
    },
    {
      "group": "Custom OutputFormat behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":30})"
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
          "expected": "[ ]",
          "description": "A custom OutputFormat that collects ValidationMessage objects into a List. For a valid document the list should be empty."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "objectNode({\"age\":\"thirty\"})"
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
          "expected": "[\"age: integer expected\"]",
          "description": "Using the same CollectingListFormat, the invalid document should produce a list containing a single validation message."
        }
      ]
    }
  ]
}