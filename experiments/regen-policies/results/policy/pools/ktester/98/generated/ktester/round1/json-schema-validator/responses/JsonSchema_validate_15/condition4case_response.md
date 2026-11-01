{
  "testCases": [
    {
      "group": "Null and Invalid Parameters",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": null },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException with message \"input must not be null\"",
          "description": "Validate that passing a null JSON string triggers a NullPointerException before any deserialization occurs."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": null },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException with message \"inputFormat must not be null\"",
          "description": "Validate that a null InputFormat argument is rejected early."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": null },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException with message \"output format must not be null\"",
          "description": "Validate that a null OutputFormat argument causes a NullPointerException."
        }
      ]
    },
    {
      "group": "Deserialization Errors",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "%ZZInvalid%" },
            { "parameter": "inputFormat", "value": "UrlEncodedInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "UnsupportedEncodingException wrapped in JsonSchemaException",
          "description": "When the input string cannot be URL‑decoded, the internal deserialize method throws UnsupportedEncodingException which should be propagated as a JsonSchemaException."
        },
        {
          "input": [
            { "parameter": "input", "value": "not a json" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "JsonProcessingException wrapped in JsonSchemaException",
          "description": "Invalid JSON syntax should cause the deserialization step to throw a parsing exception that is wrapped and re‑thrown."
        }
      ]
    },
    {
      "group": "Execution Context Customizer Branches",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"age\":30}" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true))" }
          ],
          "expected": "Output format receives a ValidationResult containing only the first validation error (fail‑fast mode)",
          "description": "Custom ExecutionContext sets fail‑fast to true; validation should stop after the first error."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"age\":30}" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<>()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Output format receives a ValidationResult with all errors (default non‑fail‑fast)",
          "description": "When no customizer is supplied, the default ExecutionContext is used (fail‑fast disabled)."
        }
      ]
    },
    {
      "group": "Validation Success vs Failure",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}" },
            { "parameter": "inputFormat", "value": "JsonSchemaInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "Empty Set (no validation messages)",
          "description": "A valid JSON document that conforms to the supplied schema should result in an empty set of ValidationMessage."
        },
        {
          "input": [
            { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}" },
            { "parameter": "inputFormat", "value": "JsonSchemaInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "Set containing at least one ValidationMessage indicating type mismatch",
          "description": "A JSON document that violates the schema (e.g., age is a string) should produce a non‑empty set of ValidationMessage."
        }
      ]
    },
    {
      "group": "Different InputFormats",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name=John&age=30" },
            { "parameter": "inputFormat", "value": "FormUrlEncodedInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "Empty Set (valid after conversion to JSON)",
          "description": "When using a FormUrlEncodedInputFormat the method should decode the string, convert to JSON and validate successfully."
        },
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: thirty" },
            { "parameter": "inputFormat", "value": "YamlInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "Set containing a ValidationMessage about the non‑numeric age",
          "description": "YAML input format should be deserialized correctly and then validated, producing an error for the invalid age value."
        }
      ]
    }
  ]
}