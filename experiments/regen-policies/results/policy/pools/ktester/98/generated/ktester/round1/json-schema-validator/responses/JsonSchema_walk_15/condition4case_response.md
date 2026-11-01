{
  "testCases": [
    {
      "group": "Successful walk with validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult containing no ValidationMessage (i.e., successful validation)",
          "description": "When a well‑formed JSON string is supplied, the input format is JSON, validation flag is true and no customizer is provided, the method should deserialize the input, invoke walkAtNodeInternal and return a ValidationResult indicating successful validation."
        }
      ]
    },
    {
      "group": "Successful walk with validation disabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"age\\\":30}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "false" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult with empty messages (validation skipped)",
          "description": "When validation flag is false the walk should still deserialize the JSON and return a ValidationResult, but no schema validation should be performed, resulting in an empty message set."
        }
      ]
    },
    {
      "group": "Walk with custom ExecutionContextCustomizer",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"id\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")" }
          ],
          "expected": "ValidationResult and the ExecutionContext contains attribute \"custom\" with value \"value\"",
          "description": "A non‑null ExecutionContextCustomizer should be invoked before walking the node. The test verifies that the customizer mutates the context as expected and that the walk still returns a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Walk with null ExecutionContext",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "input", "value": "\"{\\\"a\\\":true}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "If the supplied ExecutionContext is null the method should throw a NullPointerException before attempting deserialization."
        }
      ]
    },
    {
      "group": "Walk with unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"some text\"" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "UnsupportedOperationException (or IOException) indicating that the format cannot be parsed",
          "description": "When an InputFormat that the deserialize method does not support is provided, the method should propagate the appropriate exception."
        }
      ]
    },
    {
      "group": "Walk with malformed JSON input",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"unclosed\": [1,2,3\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new CollectingOutputFormat<ValidationResult>()" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonProcessingException (or IOException) thrown by deserialize",
          "description": "Malformed JSON should cause the deserialize step to fail, resulting in an exception that bubbles up from walk."
        }
      ]
    },
    {
      "group": "Walk with custom OutputFormat returning domain object",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"type\\\":\\\"string\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> msgs) { return msgs.isEmpty()?\"OK\":\"FAIL\"; } }" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "\"OK\"",
          "description": "When a custom OutputFormat is supplied that converts the validation messages into a simple string, walk should return that string instead of a ValidationResult."
        }
      ]
    }
  ]
}