{
  "groups": [
    {
      "group": "Null ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the supplied ExecutionContext is null the method should fail early (or create a default context). The test asserts that a NullPointerException is thrown, confirming the null‑check branch is exercised."
        }
      ]
    },
    {
      "group": "Valid JSON Input – Validate = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"age\\\":30}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true",
          "description": "A well‑formed JSON string with validate flag true should be deserialized successfully and passed to walkAtNodeInternal, resulting in a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid JSON Input – Validate = false",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"age\\\":30}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true (no validation performed)",
          "description": "When validate is false the schema walk should still succeed but skip validation logic; this branch verifies that the boolean flag is correctly propagated."
        }
      ]
    },
    {
      "group": "YAML Input – Validate = true",
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
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true",
          "description": "Ensures the method correctly selects the YAML deserialization path (different branch inside deserialize) and validates the resulting node."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"<xml></xml>\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException or custom SchemaException indicating unsupported format",
          "description": "Passing a null (or unsupported) InputFormat forces the deserialize method to take the error branch, which should result in an exception."
        }
      ]
    },
    {
      "group": "Deserialization throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{invalid json\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IOException wrapped in JsonSchemaException (or ValidationResult with errors)",
          "description": "Malformed JSON triggers an IOException inside deserialize; the test verifies that this exception propagates (or is captured) as defined by the implementation."
        }
      ]
    }
  ]
}