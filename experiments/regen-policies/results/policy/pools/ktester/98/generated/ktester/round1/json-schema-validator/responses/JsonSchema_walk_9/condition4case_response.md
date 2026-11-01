{
  "testCases": [
    {
      "group": "Valid input, validation enabled, default execution context",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors",
          "description": "A well‑formed JSON document that conforms to the schema should walk the tree, perform validation and return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid input, validation disabled, default execution context",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors (validation flag ignored)",
          "description": "When the validate flag is false the walk method should still parse the input and return a successful result without performing schema validation."
        }
      ]
    },
    {
      "group": "Invalid JSON input – deserialization failure",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{invalidJson: true," },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains a parsing error",
          "description": "Malformed JSON should cause the internal deserialize method to throw an exception; walk must catch it and return a ValidationResult indicating failure."
        }
      ]
    },
    {
      "group": "Null input string – argument validation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": null },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException (or ValidationResult.success == false with a null‑input error message)",
          "description": "Passing a null input should be rejected early, either by throwing NPE or by returning a failed ValidationResult."
        }
      ]
    },
    {
      "group": "Unsupported input format – branch for format handling",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "<root><a>1</a></root>" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage mentions unsupported format",
          "description": "If the supplied InputFormat is not supported by the deserialize method, walk should return a failure result indicating the problem."
        }
      ]
    },
    {
      "group": "Non‑null ExecutionContextCustomizer – customizer invoked",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setAttribute(\"custom\", true)" }
          ],
          "expected": "ValidationResult.success == true and the ExecutionContext used during walk contains attribute \"custom\" = true",
          "description": "When a custom ExecutionContextCustomizer is supplied it must be invoked; the test verifies that the customizer mutates the context as expected."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer throws exception – error propagation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }" }
          ],
          "expected": "RuntimeException propagated (or ValidationResult.success == false with a customizer error message)",
          "description": "If the customizer throws an exception, walk should not swallow it silently; the test confirms that the exception bubbles up or is reported in the result."
        }
      ]
    }
  ]
}