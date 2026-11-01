{
  "testCases": [
    {
      "group": "Valid Input Scenarios",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
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
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed JSON string that conforms to the schema. No custom execution context is supplied, so the default context is used."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"name: John\\nage: 30\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed YAML document that conforms to the schema, using a different InputFormat branch."
        }
      ]
    },
    {
      "group": "Invalid Input Scenarios",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":\\\"thirty\\\"}\""
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
          "expected": "A non‑empty Set<ValidationMessage> containing at least one message about type mismatch for property 'age'.",
          "description": "JSON is syntactically correct but violates the schema (age should be an integer). Tests the validation branch that collects schema violations."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\":30}\""
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
          "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because the JSON is malformed.",
          "description": "Malformed JSON triggers the deserialization error branch."
        }
      ]
    },
    {
      "group": "Null / Exception Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "null"
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
          "expected": "Throws IllegalArgumentException with message indicating that input must not be null.",
          "description": "Ensures the method validates the mandatory 'input' argument before deserialization."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{}\""
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
          "expected": "Throws IllegalArgumentException (or NullPointerException) because inputFormat is required.",
          "description": "Covers the branch that checks for a null InputFormat."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"%E4%BD%A0%E5%A5%BD\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws UnsupportedEncodingException wrapped in a ValidationException when URL decoding fails.",
          "description": "Tests the branch that catches UnsupportedEncodingException from URLDecoder."
        }
      ]
    },
    {
      "group": "Custom Execution Context",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "A Set<ValidationMessage> that may be truncated after the first error (if any). Since the input is valid, the set is empty.",
          "description": "Provides a custom ExecutionContextCustomizer that enables fail‑fast mode, exercising the branch where the customizer is applied to the ExecutionContext before validation."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":\\\"thirty\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setReportAllErrors(false)"
            }
          ],
          "expected": "A Set<ValidationMessage> containing at most one error message because the customizer disables reporting of all errors.",
          "description": "Customizes the execution context to stop collecting further errors after the first violation, covering the branch that respects the customized context settings."
        }
      ]
    }
  ]
}