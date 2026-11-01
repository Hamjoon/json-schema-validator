{
  "testCases": [
    {
      "group": "Validation error – missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Set<ValidationMessage>>() { public Set<ValidationMessage> apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> of size > 0 containing a message about missing required property 'age'",
          "description": "The schema requires the property 'age'. Providing JSON without it should produce a validation error."
        }
      ]
    },
    {
      "group": "Validation error – type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":\\\"thirty\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> msgs) { return new ArrayList<>(msgs); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "List<ValidationMessage> of size > 0 containing a message about type mismatch for property 'age'",
          "description": "The schema defines 'age' as an integer. Supplying a string should trigger a type‑validation error."
        }
      ]
    },
    {
      "group": "Validation error – additional property not allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30,\\\"extra\\\":\\\"value\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Set<ValidationMessage>>() { public Set<ValidationMessage> apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> of size > 0 containing a message about disallowed additional property 'extra'",
          "description": "The schema has \"additionalProperties\": false. Supplying an extra field should cause a validation error."
        }
      ]
    },
    {
      "group": "Validation succeeds when customizer relaxes additionalProperties",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30,\\\"extra\\\":\\\"value\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> msgs) { return new ArrayList<>(msgs); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.getConfig().setAdditionalProperties(true)"
            }
          ],
          "expected": "List<ValidationMessage> of size 0 (no validation errors)",
          "description": "The customizer sets the configuration to allow additional properties, so the same input that previously failed now validates successfully."
        }
      ]
    },
    {
      "group": "Empty input string – JSON parsing error",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "JsonProcessingException wrapped in a RuntimeException",
          "description": "An empty string is not valid JSON; deserialization should fail and the exception should propagate."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat value",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"a\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNKNOWN"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException indicating unsupported InputFormat",
          "description": "Passing an InputFormat value that the validator does not recognise should result in an IllegalArgumentException."
        }
      ]
    }
  ]
}