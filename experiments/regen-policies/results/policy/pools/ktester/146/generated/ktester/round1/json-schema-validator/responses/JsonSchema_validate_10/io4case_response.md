{
  "testCases": [
    {
      "group": "Null InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException or NullPointerException",
          "description": "The method should reject a null InputFormat before attempting deserialization, because the format is required to know how to parse the string."
        }
      ]
    },
    {
      "group": "Whitespace‑only input string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"   \\t\\n\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing a parsing error (e.g., \"No content to map due to end-of-input\")",
          "description": "A string that contains only whitespace is not valid JSON. The method must attempt deserialization, fail, and return a validation message describing the empty content."
        }
      ]
    },
    {
      "group": "Schema constraint violation – missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing a message like \"required property 'name' not found\"",
          "description": "The JsonSchema instance is built from a schema that declares a required property \"name\". Supplying JSON without that property should cause the validator to return a ValidationMessage indicating the missing required field."
        }
      ]
    },
    {
      "group": "Schema constraint violation with execution customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"customizerInvoked\", true)"
            }
          ],
          "expected": "Set<ValidationMessage> containing a required‑property error and ctx.getAttribute(\"customizerInvoked\") == true",
          "description": "Even when validation fails because the JSON does not satisfy the schema, the supplied Consumer must still be executed. The test verifies that the custom attribute is set while the validation set contains the expected schema‑violation message."
        }
      ]
    },
    {
      "group": "JSON input with UTF‑8 BOM prefix",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"\\uFEFF{\\\"name\\\":\\\"John\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Empty Set<ValidationMessage>",
          "description": "A UTF‑8 Byte Order Mark (BOM) at the beginning of the JSON string should be ignored by the deserializer. The method must successfully parse the document and return no validation errors."
        }
      ]
    },
    {
      "group": "Execution customizer throws checked exception wrapped in RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"valid\":true}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { try { ctx.setAttribute(\"x\", 1); } catch (Exception e) { throw new RuntimeException(e); } }"
            }
          ],
          "expected": "RuntimeException (the original exception is propagated)",
          "description": "If the consumer catches a checked exception and re‑throws it as a RuntimeException, the validate method must let that RuntimeException bubble up unchanged."
        }
      ]
    }
  ]
}