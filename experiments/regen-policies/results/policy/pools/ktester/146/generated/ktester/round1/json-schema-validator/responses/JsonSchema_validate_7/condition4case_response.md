{
  "group": "validate(ExecutionContext, JsonNode, OutputFormat) – delegation behavior",
  "cases": [
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "MockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "MockJsonNode{\"type\":\"object\"}"
        },
        {
          "parameter": "format",
          "value": "MockOutputFormat returning \"OK\""
        }
      ],
      "expected": "OK",
      "description": "Normal flow – non‑null ExecutionContext, JsonNode and OutputFormat. The method should delegate to the overloaded validate(executionContext, node, format, null) and return the value produced by the OutputFormat (here the mock returns the string \"OK\")."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "null"
        },
        {
          "parameter": "node",
          "value": "MockJsonNode{\"type\":\"object\"}"
        },
        {
          "parameter": "format",
          "value": "MockOutputFormat"
        }
      ],
      "expected": "NullPointerException",
      "description": "executionContext is null – the delegate call should propagate a NullPointerException (the overloaded method expects a non‑null ExecutionContext)."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "MockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "null"
        },
        {
          "parameter": "format",
          "value": "MockOutputFormat"
        }
      ],
      "expected": "NullPointerException",
      "description": "node is null – the method should fail early with a NullPointerException because the underlying validation logic cannot operate on a null JsonNode."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "MockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "MockJsonNode{\"type\":\"object\"}"
        },
        {
          "parameter": "format",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "format is null – the method must throw a NullPointerException as it cannot forward a null OutputFormat to the overloaded implementation."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "MockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "MockJsonNode{\"type\":\"array\"}"
        },
        {
          "parameter": "format",
          "value": "MockOutputFormat capturing ExecutionContext and Node"
        }
      ],
      "expected": "capturedExecutionContext == MockExecutionContext && capturedNode == MockJsonNode{\"type\":\"array\"}",
      "description": "Verify that the delegated call passes the original ExecutionContext and JsonNode unchanged (no ExecutionContextCustomizer is supplied). The mock OutputFormat records the parameters it receives; the test asserts that they match the inputs."
    }
  ]
}