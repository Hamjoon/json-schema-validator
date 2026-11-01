{
  "testCases": [
    {
      "group": "Valid JSON input with validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage returned",
          "description": "A well‑formed JSON string is supplied, the format is JSON, validation is turned on and no customizer is provided. The method should deserialize the string, walk the schema and return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid JSON input with validation disabled",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\",\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == true and no validation performed (internal flag should skip validators)",
          "description": "When the `validate` flag is false the schema walk should still succeed but must not execute any validators. The result should indicate success."
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
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains deserialization error",
          "description": "The supplied string is not a valid JSON document. The `deserialize` step should throw an exception which is caught and translated into a failed ValidationResult."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "null" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown indicating unsupported InputFormat",
          "description": "If the `inputFormat` argument is null (or a value not supported by the implementation) the method should fail fast with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Consumer customizer modifies ExecutionContext",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "consumer that sets ExecutionContext.setCustomFlag(true)" }
          ],
          "expected": "ValidationResult.success == true and ExecutionContext.customFlag == true after walk",
          "description": "A non‑null Consumer should be invoked with the freshly created ExecutionContext before walking the schema. The test verifies that the consumer is called and can affect the context."
        }
      ]
    },
    {
      "group": "Null consumer (no customisation)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == true and ExecutionContext remains unchanged",
          "description": "When the consumer argument is null the method must skip the customisation step without throwing NPE."
        }
      ]
    },
    {
      "group": "YAML input format with valid content",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 30" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage returned",
          "description": "The method must correctly deserialize YAML when the format is YAML, then walk the schema and succeed."
        }
      ]
    },
    {
      "group": "Deserialization throws UnsupportedEncodingException",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%E0%A4%A" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains UnsupportedEncodingException information",
          "description": "If URLDecoder fails during deserialization (e.g., malformed percent‑encoding), the method should capture the exception and return a failed ValidationResult."
        }
      ]
    }
  ]
}