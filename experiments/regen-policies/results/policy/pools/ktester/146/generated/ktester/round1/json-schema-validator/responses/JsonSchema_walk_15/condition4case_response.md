Invalid control character at: line 13 column 55 (char 460)
{
  "testCases": [
    {
      "group": "Successful walk with validation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()   // returns node.toString()"
            },
            { "parameter": "validate", "value": true },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\",\"value\")   // no exception"
            }
          ],
          "expected": "\"{\\\"name\\\":\\\"John\\\"}\"",
          "description": "Valid JSON string is deserialized and walkAtNodeInternal is invoked with validation enabled. The customizer runs without error and the output format returns the node's string representation."
        }
      ]
    },
    {
      "group": "Successful walk without validation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{\"age\":30}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new IdentityOutputFormat<>()   // returns the JsonNode itself"
            },
            { "parameter": "validate", "value": false },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonNode representing {\"age\":30}",
          "description": "When validation is disabled the walk proceeds but no validators are executed. The output format simply returns the JsonNode."
        }
      ]
    },
    {
      "group": "Deserialize error – malformed JSON",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{invalidJson:}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "com.fasterxml.jackson.core.JsonParseException",
          "description": "Malformed JSON triggers an exception inside deserialize; the method should propagate the exception."
        }
      ]
    },
    {
      "group": "Deserialize error – unsupported InputFormat",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "<xml></xml>" },
            { "parameter": "inputFormat", "value": "InputFormat.XML" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "UnsupportedOperationException",
          "description": "If the supplied InputFormat is not supported by deserialize, an UnsupportedOperationException (or similar) should be thrown."
        }
      ]
    },
    {
      "group": "Null arguments handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "input", "value": "{\"a\":1}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "A null ExecutionContext should cause a NullPointerException when walkAtNodeInternal attempts to use it."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "A null input string should be rejected by deserialize, resulting in a NullPointerException."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{\"b\":2}" },
            { "parameter": "inputFormat", "value": "null" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "A null InputFormat leads to a NullPointerException inside deserialize."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{\"c\":3}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "outputFormat", "value": "null" },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "A null OutputFormat should cause a NullPointerException when walkAtNodeInternal attempts to invoke it."
        }
      ]
    },
    {
      "group": "OutputFormat exception propagation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "input", "value": "{\"d\":4}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ThrowingOutputFormat<>()   // throws IllegalStateException when apply() is called"
            },
            { "parameter": "validate", "value": true },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalStateException",
          "description": "If the OutputFormat throws an exception during processing, walk should propagate that exception."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer invocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "validExecutionContext   // with a mutable map attribute store"
            },
            { "parameter": "input", "value": "{\"e\":5}" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            {
              "parameter": "outputFormat",
              "value": "new ToStringOutputFormat<>()"
            },
            { "parameter": "validate", "value": true },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"customKey\",\"customValue\")"
            }
          ],
          "expected": "ExecutionContext contains attribute customKey=customValue after walk",
          "description": "The customizer lambda should be executed exactly once before the walk proceeds, modifying the context as expected."
        }
      ]
    }
  ]
}