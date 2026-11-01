{
  "testCases": [
    {
      "group": "Normal validation – JSON input, successful walk",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> msgs) { return new ArrayList<>(msgs); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig())"
            }
          ],
          "expected": "List<ValidationMessage> of size 0 (no validation errors)",
          "description": "A well‑formed JSON document that complies with the schema should be deserialized, walked and produce an empty list of validation messages."
        }
      ]
    },
    {
      "group": "Normal validation – YAML input, successful walk",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"name: John\\nage: 30\""},
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Set<ValidationMessage>>() { public Set<ValidationMessage> apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Set<ValidationMessage> of size 0",
          "description": "A well‑formed YAML document should be deserialized via YamlMapperFactory and validated without errors. The executionCustomizer is optional and may be null."
        }
      ]
    },
    {
      "group": "Invalid JSON – parsing error",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30\""},
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonProcessingException wrapped in a RuntimeException",
          "description": "The JSON string is malformed (missing closing brace). deserialize() should throw a JsonProcessingException which propagates as an unchecked exception from validate(...)."
        }
      ]
    },
    {
      "group": "Invalid YAML – parsing error",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"name: John\\nage\""},
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonProcessingException wrapped in a RuntimeException",
          "description": "The YAML content is syntactically invalid; deserialization should fail and the method should surface the exception."
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
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null JSON/YAML string should cause a NullPointerException before deserialization."
        }
      ]
    },
    {
      "group": "Null InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"a\\\":1}\"" },
            { "parameter": "inputFormat", "value": "null" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) indicating unsupported format",
          "description": "The method should validate that inputFormat is not null and throw an appropriate exception."
        }
      ]
    },
    {
      "group": "Null OutputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"a\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "null" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "When the OutputFormat argument is null, the method cannot apply the result and should throw NPE."
        }
      ]
    },
    {
      "group": "Null ExecutionContextCustomizer – should be handled gracefully",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"a\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> msgs) { return new ArrayList<>(msgs); } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "List<ValidationMessage> of size 0",
          "description": "The method must accept a null executionCustomizer and still perform validation using the default ExecutionContext."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer throws runtime exception",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"a\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new IllegalStateException(\"Customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException propagated from the customizer",
          "description": "If the provided ExecutionContextCustomizer throws an exception, validate(...) should let it propagate, allowing the caller to see the failure."
        }
      ]
    },
    {
      "group": "Unsupported charset handling (internal branch)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"%E4%BD%A0%E5%A5%BD\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Set<ValidationMessage> of size 0 (URL decoding succeeds) – verifies the branch that catches UnsupportedEncodingException does not fire because StandardCharsets.UTF_8 is always supported.",
          "description": "The method internally uses URLDecoder.decode(..., StandardCharsets.UTF_8). This branch ensures no UnsupportedEncodingException is thrown; the test confirms normal operation."
        }
      ]
    }
  ]
}