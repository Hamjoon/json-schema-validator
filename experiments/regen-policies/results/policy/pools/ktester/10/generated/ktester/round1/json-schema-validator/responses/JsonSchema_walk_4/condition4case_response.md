{
  "tests": [
    {
      "group": "Valid walk with validation (JSON input, validate = true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
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
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isSuccess() == true and no ValidationMessage entries",
          "description": "A well‑formed JSON document is supplied, the format is JSON and validation is requested. The method should deserialize the string and invoke walkAtNodeInternal with validation enabled, returning a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid walk without validation (JSON input, validate = false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"id\\\":1,\\\"active\\\":true}\""
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
          "expected": "ValidationResult with isSuccess() == true (or at least no validation errors) – the walk should complete even though validation is disabled",
          "description": "Even when validation is turned off, walk should still deserialize the input and walk the schema tree without performing validation checks."
        }
      ]
    },
    {
      "group": "Invalid JSON input – deserialization failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"unclosed\": [1,2,3}\""
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
          "expected": "throws com.fasterxml.jackson.core.JsonProcessingException (or a subclass) from deserialize",
          "description": "The JSON string is syntactically incorrect; deserialize should throw a JsonProcessingException which propagates out of walk."
        }
      ]
    },
    {
      "group": "Null input string – handling of null payload",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
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
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "throws NullPointerException or IllegalArgumentException (depending on deserialize implementation)",
          "description": "Passing a null input string should cause deserialize to reject the argument; the test verifies that the appropriate exception is thrown."
        }
      ]
    },
    {
      "group": "YAML input format – successful deserialization and validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
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
          "expected": "ValidationResult with isSuccess() == true and no ValidationMessage entries",
          "description": "A correct YAML document is supplied; deserialize should use YamlMapperFactory and produce a JsonNode that can be walked and validated successfully."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat – fallback or error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "JsonSchema.createExecutionContext()"
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
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "throws IllegalArgumentException (or a custom exception) because inputFormat is null/unsupported",
          "description": "When the inputFormat argument is null or not recognized, deserialize should reject the call, causing walk to throw an exception."
        }
      ]
    }
  ]
}