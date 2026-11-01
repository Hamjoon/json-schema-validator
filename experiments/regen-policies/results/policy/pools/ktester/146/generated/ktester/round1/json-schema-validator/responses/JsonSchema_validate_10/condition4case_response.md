{
  "testCases": [
    {
      "group": "Valid JSON input without customizer",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Empty Set<ValidationMessage> (no validation errors)",
          "description": "A well‑formed JSON document is supplied. The method should deserialize the string, run the validators and return an empty validation set because the default schema accepts the document. No execution customizer is provided."
        }
      ]
    },
    {
      "group": "Invalid JSON syntax",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Set<ValidationMessage> containing a parsing error message (e.g., \"Invalid JSON syntax\")",
          "description": "The JSON string is malformed (missing closing brace). The method should catch the parsing exception and return a validation message describing the syntax error."
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
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the supplied input string is null the method is expected to throw an IllegalArgumentException before attempting deserialization."
        }
      ]
    },
    {
      "group": "Empty input string",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Set<ValidationMessage> containing a parsing error (empty content)",
          "description": "An empty string cannot be parsed as JSON; the method should return a validation message indicating that the input is empty or not a valid JSON document."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.UNSUPPORTED" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "UnsupportedOperationException",
          "description": "If an InputFormat that the implementation does not support is passed, the method should throw an UnsupportedOperationException (or similar) before deserialization."
        }
      ]
    },
    {
      "group": "Execution customizer modifies context",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "executionCustomizer", "value": "ctx -> ctx.setAttribute(\"testFlag\", true)" }
          ],
          "expected": "Empty Set<ValidationMessage> and ctx.getAttribute(\"testFlag\") == true",
          "description": "A non‑null consumer is provided that sets a custom attribute in the ExecutionContext. The method must invoke the consumer before validation and the attribute should be present after the call."
        }
      ]
    },
    {
      "group": "Execution customizer throws runtime exception",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"valid\":true}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "executionCustomizer", "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }" }
          ],
          "expected": "RuntimeException with message \"customizer failure\"",
          "description": "When the supplied consumer throws an unchecked exception, the method should propagate that exception and not swallow it."
        }
      ]
    },
    {
      "group": "YAML input format (valid document)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: 25" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Empty Set<ValidationMessage>",
          "description": "The method must correctly deserialize a well‑formed YAML document when InputFormat.YAML is supplied and return no validation errors."
        }
      ]
    },
    {
      "group": "YAML input format (invalid document)",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "name: John\nage: [unclosed" },
            { "parameter": "inputFormat", "value": "InputFormat.YAML" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Set<ValidationMessage> containing a YAML parsing error",
          "description": "A malformed YAML string should cause the deserialization step to fail, and the method should return a validation message describing the parsing problem."
        }
      ]
    }
  ]
}