{
  "testCases": [
    {
      "group": "Valid JSON input – validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Ensures that a well‑formed JSON string is deserialized correctly, the walk proceeds with validation enabled and the output format collects a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid JSON input – validation disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"id\\\":123}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true (no validation performed)",
          "description": "Verifies that when the `validate` flag is false the walk still returns a successful result without applying any validators."
        }
      ]
    },
    {
      "group": "Null executionContext – should be created internally",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"type\\\":\\\"string\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true (method creates a default ExecutionContext)",
          "description": "Checks that a `null` ExecutionContext does not cause NPE and the method internally creates a fresh context before walking the node."
        }
      ]
    },
    {
      "group": "Empty input string – deserialization should produce empty node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult containing a single ValidationMessage about empty input",
          "description": "Ensures that an empty string is handled gracefully – the deserializer returns a null/empty node and the walk reports a meaningful validation message."
        }
      ]
    },
    {
      "group": "Invalid JSON – deserialization throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"unclosed\":\"value\"\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "throws IOException (or JsonProcessingException) wrapped in a RuntimeException",
          "description": "Confirms that malformed JSON triggers the deserialization exception path, which should be propagated as a runtime exception from `walk`."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat – should raise IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"some text\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'InputFormat cannot be null'",
          "description": "Validates that a `null` InputFormat is rejected early, exercising the branch that checks for unsupported or missing format."
        }
      ]
    },
    {
      "group": "OutputFormat that throws during processing",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"a\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new ThrowingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "RuntimeException propagated from OutputFormat",
          "description": "Ensures that any exception thrown by the supplied OutputFormat is not swallowed and bubbles up to the caller."
        }
      ]
    },
    {
      "group": "YAML input format – successful walk",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"name: Jane\\nage: 25\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true",
          "description": "Tests the branch where the method selects a non‑JSON deserializer (YAML) and verifies normal processing."
        }
      ]
    },
    {
      "group": "Boolean flag false with null outputFormat – should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"x\\\":10}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "NullPointerException with message containing 'outputFormat'",
          "description": "Verifies that a null OutputFormat is not accepted, exercising the null‑check branch before delegating to walkAtNodeInternal."
        }
      ]
    }
  ]
}