{
  "groups": [
    {
      "group": "Valid inputs – JSON format",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with no validation messages (isSuccess == true)",
          "description": "Walk a well‑formed JSON string with validation enabled – should deserialize correctly and return a successful ValidationResult."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": "false" }
          ],
          "expected": "ValidationResult with no validation messages (isSuccess == true)",
          "description": "Walk a well‑formed JSON string with validation disabled – the method still deserialises and returns a successful result."
        }
      ]
    },
    {
      "group": "Valid inputs – YAML format",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "\"name: John\\nage: 30\""},
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with no validation messages (isSuccess == true)",
          "description": "Walk a well‑formed YAML string – the deserialize method should handle YAML and the walk should succeed."
        }
      ]
    },
    {
      "group": "Invalid JSON syntax",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
          "description": "Malformed JSON should cause the deserialize step to throw a parsing exception; the test verifies that the exception propagates."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "throws NullPointerException",
          "description": "Passing a null input string should result in a NullPointerException during deserialization."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "jsonSchema.createExecutionContext()" },
            { "parameter": "input", "value": "\"<root></root>\"" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "throws IllegalArgumentException or UnsupportedOperationException",
          "description": "If an InputFormat that the library does not support (e.g., XML) is supplied, deserialize should reject it with an appropriate exception."
        }
      ]
    }
  ]
}