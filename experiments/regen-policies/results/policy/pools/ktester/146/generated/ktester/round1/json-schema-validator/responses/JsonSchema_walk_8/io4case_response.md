{
  "generatedTests": [
    {
      "group": "Empty JSON input",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains deserialization error (empty input)",
          "description": "An empty string is supplied for a JSON format. Deserialization should fail, producing a failed ValidationResult with an appropriate error message."
        }
      ]
    },
    {
      "group": "Whitespace JSON input",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"   \\n\\t  \"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains deserialization error (whitespace only)",
          "description": "A JSON string that contains only whitespace should be treated as invalid JSON, causing deserialization to fail."
        }
      ]
    },
    {
      "group": "Invalid YAML input – deserialization failure",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: [30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains deserialization error (YAML syntax)",
          "description": "YAML input with a syntax error should trigger a deserialization exception, which must be reported as a failed ValidationResult."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown indicating that input string must not be null",
          "description": "Passing a null reference for the JSON/YAML string should cause the method to fail fast with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat value (e.g., XML)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "<root><name>John</name></root>" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown indicating unsupported InputFormat",
          "description": "If an InputFormat that the library does not handle (e.g., XML) is supplied, the method should throw an IllegalArgumentException before any processing."
        }
      ]
    },
    {
      "group": "Consumer throws RuntimeException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "consumer that throws RuntimeException(\"customizer failure\")" }
          ],
          "expected": "RuntimeException propagated (test should expect the exception)",
          "description": "When the supplied Consumer throws a RuntimeException, the walk method must not swallow it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "Validation fails due to schema constraints",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains 'required property name' (or similar)",
          "description": "The schema (assumed to require a \"name\" property) is violated by the provided JSON. The walk should perform validation and return a failed result with the appropriate message."
        }
      ]
    },
    {
      "group": "URL‑encoded JSON input decoded correctly",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage returned",
          "description": "A JSON string that is URL‑encoded should be correctly decoded by the deserialization step and validated successfully."
        }
      ]
    },
    {
      "group": "Non‑null Consumer that does not modify context",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "consumer that reads ExecutionContext but makes no changes" }
          ],
          "expected": "ValidationResult.success == true and ExecutionContext remains in default state",
          "description": "A Consumer is supplied that accesses the ExecutionContext without mutating it. The walk should succeed and the context should stay unchanged."
        }
      ]
    }
  ]
}