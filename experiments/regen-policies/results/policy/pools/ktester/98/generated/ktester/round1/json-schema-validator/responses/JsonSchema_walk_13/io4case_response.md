{
  "testCases": [
    {
      "group": "Null input string – should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "null"
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
          "expected": "NullPointerException with message containing 'input'",
          "description": "When the supplied JSON string is null the deserializer cannot operate and the method should fail fast with a NullPointerException."
        }
      ]
    },
    {
      "group": "Whitespace‑only input – treated as empty JSON node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"   \\n\\t  \""
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
          "expected": "ValidationResult with isSuccess() == true (no validation performed on empty node)",
          "description": "A string that contains only whitespace should be deserialized to an empty JsonNode; with validation disabled the walk should succeed without errors."
        }
      ]
    },
    {
      "group": "Invalid YAML – deserialization throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"name: John\\nage: [30,]\""
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
          "expected": "RuntimeException wrapping an IOException (or JsonProcessingException) from the YAML parser",
          "description": "Malformed YAML must cause the private `deserialize` method to throw an IOException, which is wrapped and propagated by `walk`."
        }
      ]
    },
    {
      "group": "Null OutputFormat with validation enabled – should throw NullPointerException",
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
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException with message containing 'outputFormat'",
          "description": "Even when validation is requested, a null OutputFormat is illegal and must be rejected before any walking occurs."
        }
      ]
    },
    {
      "group": "Validation failure – schema violation reported",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"age\\\":25}\""
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
          "expected": "ValidationResult where isSuccess() == false and contains a ValidationMessage about missing required property 'name'",
          "description": "The schema (configured for the test) requires a string property `name`. Supplying JSON without that property should cause validation to fail and the CollectingOutputFormat to return a result with the appropriate error."
        }
      ]
    },
    {
      "group": "Custom OutputFormat returning non‑ValidationResult type",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"[1,2,3]\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new NodeCountOutputFormat()"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "Integer value 3 (the number of elements in the JSON array)",
          "description": "`NodeCountOutputFormat` implements `OutputFormat<Integer>` and counts the number of JSON nodes visited. With validation disabled the walk should still invoke the output format and return the count."
        }
      ]
    },
    {
      "group": "Unsupported non‑null InputFormat – should raise IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"some plain text\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNKNOWN"
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
          "expected": "IllegalArgumentException with message containing 'Unsupported InputFormat'",
          "description": "Passing an InputFormat value that the `deserialize` method does not recognise must trigger an early argument‑validation failure."
        }
      ]
    },
    {
      "group": "OutputFormat throws while validation flag is false – exception propagates",
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
              "value": "new ThrowingOutputFormat<ValidationResult>()"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "RuntimeException propagated from ThrowingOutputFormat",
          "description": "Even when validation is disabled, the supplied OutputFormat is still invoked; any exception it throws must not be swallowed."
        }
      ]
    }
  ]
}