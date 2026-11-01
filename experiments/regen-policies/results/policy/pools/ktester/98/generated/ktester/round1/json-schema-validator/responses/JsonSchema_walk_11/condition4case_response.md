{
  "testCases": [
    {
      "group": "Normal operation – validation enabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode().put(\"name\", \"John\")" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(ValidationResult r) { return \"OK\"; } }"
            },
            { "parameter": "validate", "value": "true" },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "\"OK\"",
          "description": "Walk the schema with a valid JSON node, validation turned on, a simple OutputFormat that returns a constant string and a customizer that mutates the ExecutionContext. The method should return the value produced by the OutputFormat."
        }
      ]
    },
    {
      "group": "Normal operation – validation disabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode().put(\"age\", 30)" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Integer>() { public Integer apply(ValidationResult r) { return r.getMessages().size(); } }"
            },
            { "parameter": "validate", "value": "false" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "0",
          "description": "When validation is disabled the walk should skip validator execution and produce an empty ValidationResult, therefore the OutputFormat returns 0 messages."
        }
      ]
    },
    {
      "group": "Null ExecutionContext – should be created internally",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode().put(\"id\", 1)" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Boolean>() { public Boolean apply(ValidationResult r) { return true; } }"
            },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "true",
          "description": "If the supplied ExecutionContext is null the method must create a fresh context internally and continue processing. The OutputFormat returns true, confirming successful execution."
        }
      ]
    },
    {
      "group": "Null JsonNode – illegal argument",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "null" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(ValidationResult r) { return \"\"; } }"
            },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a null JsonNode should cause the method to throw IllegalArgumentException (or NullPointerException depending on implementation)."
        }
      ]
    },
    {
      "group": "Null OutputFormat – illegal argument",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode()" },
            { "parameter": "outputFormat", "value": "null" },
            { "parameter": "validate", "value": "true" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "A null OutputFormat cannot be used to produce a result, so the method should reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer is invoked",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode().put(\"flag\", true)" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(ValidationResult r) { return r.getMessages().isEmpty() ? \"OK\" : \"FAIL\"; } }"
            },
            { "parameter": "validate", "value": "true" },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"visited\", true)"
            }
          ],
          "expected": "\"OK\"",
          "description": "The provided Consumer should be called exactly once. The test verifies that after walk returns, the ExecutionContext contains the attribute set by the customizer."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer throws exception – propagation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "new ExecutionContext()" },
            { "parameter": "node", "value": "JsonNodeFactory.objectNode()" },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(ValidationResult r) { return \"unused\"; } }"
            },
            { "parameter": "validate", "value": "true" },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the Consumer throws an exception, walk should let the exception bubble up. The test expects a RuntimeException with the original message."
        }
      ]
    }
  ]
}