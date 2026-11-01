{
  "test_cases": [
    {
      "group": "OutputFormat throws RuntimeException – propagates",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ObjectNode {\"key\":\"value\"}" },
            { "parameter": "outputFormat", "value": "mockOutputFormatThrowingRuntimeException" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "RuntimeException",
          "description": "If the supplied OutputFormat throws a RuntimeException during its apply() call, walk must not catch it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "OutputFormat throws checked exception – propagates as RuntimeException",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ObjectNode {\"key\":\"value\"}" },
            { "parameter": "outputFormat", "value": "mockOutputFormatThrowingIOException" },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "RuntimeException",
          "description": "When the OutputFormat's apply method throws a checked exception (e.g., IOException), walk must wrap or re‑throw it as an unchecked RuntimeException."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer throws checked exception – propagates as RuntimeException",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ObjectNode {\"key\":123}" },
            { "parameter": "outputFormat", "value": "mockOutputFormatReturningString" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "mockConsumerThrowingIOException" }
          ],
          "expected": "RuntimeException",
          "description": "If the Consumer<ExecutionContext> supplied as executionCustomizer throws a checked exception (e.g., IOException), walk must propagate it as an unchecked RuntimeException."
        }
      ]
    },
    {
      "group": "Node is MissingNode – illegal argument",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "MissingNode.instance()" },
            { "parameter": "outputFormat", "value": "mockOutputFormatReturningString" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the supplied JsonNode is a MissingNode (Jackson's representation of a non‑existent property), walk should reject it with an IllegalArgumentException because there is no concrete instance to validate."
        }
      ]
    },
    {
      "group": "Schema $ref with unsupported encoding – IllegalArgumentException",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithRefSchema" },
            { "parameter": "node", "value": "ObjectNode {\"$ref\":\"http://example.com/schema%ZZ\"}" },
            { "parameter": "outputFormat", "value": "mockOutputFormatReturningString" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the schema contains a $ref whose URI cannot be decoded (URLDecoder throws UnsupportedEncodingException), walk should translate this into an IllegalArgumentException."
        }
      ]
    }
  ]
}