{
  "testCases": [
    {
      "group": "Successful validation – JSON input, validate true, no customizer",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
          "description": "A well‑formed JSON document that conforms to a simple schema. Validation is enabled and no customizer is supplied, so walk should return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Successful validation – YAML input, validate true, no customizer",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "type: object\nproperties:\n  age:\n    type: integer" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
          "description": "Same schema as above but supplied as YAML. Walk should correctly deserialize YAML and perform validation."
        }
      ]
    },
    {
      "group": "Validate flag false – validation is skipped",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "{\"invalid\":\"data\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.isValid == true (validation skipped) and ValidationResult.getMessages().isEmpty()",
          "description": "Even though the JSON does not match the schema, the validate flag is false, so walkAtNodeInternal should bypass validation and return a successful result."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer invoked",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setAttribute(\"custom\", true)" }
          ],
          "expected": "ValidationResult.isValid == true and ctx.getAttribute(\"custom\") == true",
          "description": "A customizer lambda that sets an attribute on the ExecutionContext. The test verifies that the customizer is executed before walking the node."
        }
      ]
    },
    {
      "group": "Invalid JSON – deserialize throws exception",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "{\"unclosedObject\": true" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws com.fasterxml.jackson.core.JsonParseException (or IOException) from deserialize",
          "description": "Malformed JSON should cause the private deserialize method to fail, propagating an exception from walk."
        }
      ]
    },
    {
      "group": "Null or empty input string",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "ValidationResult.isValid == false and ValidationResult.getMessages().size() > 0 (error about empty input)",
          "description": "An empty string cannot be deserialized into a JsonNode; the method should return a ValidationResult indicating failure."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": null },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "throws NullPointerException from deserialize",
          "description": "Passing null as the input string should result in a NullPointerException during deserialization."
        }
      ]
    }
  ]
}