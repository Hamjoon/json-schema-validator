{
  "testCases": [
    {
      "group": "Valid JSON input, default execution context and customizer",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> {}" }
          ],
          "expected": "ValidationResult indicating successful validation (no errors)",
          "description": "Standard happy‑path: a well‑formed JSON string is deserialized and walked with validation enabled. No customisation of the execution context is performed, so the result should contain an empty error set."
        }
      ]
    },
    {
      "group": "Valid YAML input, custom execution context",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "ExecutionContext.builder().setVersion(VersionFlag.V201909).build()" },
            { "parameter": "input", "value": "\"name: John\\nage: 30\"" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig())" }
          ],
          "expected": "ValidationResult with successful validation (no errors) and the custom config applied",
          "description": "Ensures that the method correctly deserialises YAML input and that a non‑default ExecutionContext (with a specific spec version) is respected during walking."
        }
      ]
    },
    {
      "group": "Invalid JSON input – deserialization failure",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"name\\\":John}\"" },  	
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> {}" }
          ],
          "expected": "Throws JsonParseException (or IOException) wrapped by ValidationException",
          "description": "The JSON string is malformed (missing quotes around the value). The deserialize step should fail, causing the walk method to propagate the parsing exception."
        }
      ]
    },
    {
      "group": "Unsupported encoding – deserialize throws UnsupportedEncodingException",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"%E0%A4%A\""},  	
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> {}" }
          ],
          "expected": "Throws UnsupportedEncodingException",
          "description": "When the input format is URL_ENCODED and the string contains an illegal percent‑encoding, the internal URLDecoder throws UnsupportedEncodingException, which should be propagated."
        }
      ]
    },
    {
      "group": "Validate flag set to false – walk should skip validation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"age\\\":\\\"twenty\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": "ctx -> {}" }
          ],
          "expected": "ValidationResult with empty error set (validation skipped)",
          "description": "Even though the JSON does not conform to the schema (age is a string), the validate flag is false, so walkAtNodeInternal should return a result without performing validation."
        }
      ]
    },
    {
      "group": "Null ExecutionContext – should use default context internally",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "input", "value": "\"{\\\"id\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "ctx -> {}" }
          ],
          "expected": "ValidationResult with successful validation (no errors)",
          "description": "When a null ExecutionContext is supplied, the method is expected to create a default context (as per library contract) and proceed without throwing NPE."
        }
      ]
    },
    {
      "group": "Null ExecutionContextCustomizer – walk should proceed with default customisation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "input", "value": "\"{\\\"type\\\":\\\"object\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "ValidationResult with successful validation (no errors)",
          "description": "A null customizer should be handled gracefully; the walk method should invoke walkAtNodeInternal with a no‑op customiser and return a normal result."
        }
      ]
    }
  ]
}