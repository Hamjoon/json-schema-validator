{
  "test_cases": [
    {
      "group": "validate(ExecutionContext, JsonNode, OutputFormat) – propagation of schema parsing exception",
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
          "expected": "JsonSchemaException",
          "description": "The JsonSchema instance was created from an invalid schema definition (e.g., contains an unknown keyword). When validate is invoked, the internal validation logic throws a JsonSchemaException. The method must not catch it; the same exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "validate(ExecutionContext, JsonNode, OutputFormat) – propagation of ExecutionContext internal error",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext that throws IllegalStateException(\"context broken\") on any method call"
            },
            {
              "parameter": "node",
              "value": "MockJsonNode{\"type\":\"string\",\"value\":\"test\"}"
            },
            {
              "parameter": "format",
              "value": "MockOutputFormat returning \"OK\""
            }
          ],
          "expected": "IllegalStateException",
          "description": "During validation the JsonSchema implementation interacts with the supplied ExecutionContext (e.g., to retrieve configuration or to record path information). The mock ExecutionContext throws an IllegalStateException when accessed. The validate method must let this exception propagate unchanged."
        }
      ]
    },
    {
      "group": "validate(ExecutionContext, JsonNode, OutputFormat) – checked exception from OutputFormat is wrapped",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "MockJsonNode{\"type\":\"number\",\"value\":123}"
            },
            {
              "parameter": "format",
              "value": "MockOutputFormat that throws IOException(\"IO failure\") in apply"
            }
          ],
          "expected": "RuntimeException caused by IOException",
          "description": "OutputFormat.apply is declared to throw a checked exception (e.g., IOException). The validate method cannot declare a checked exception, so it should wrap the IOException in a RuntimeException (or propagate as is if already unchecked). The test verifies that a RuntimeException is thrown and its cause is the original IOException."
        }
      ]
    },
    {
      "group": "validate(ExecutionContext, JsonNode, OutputFormat) – handling of Jackson MissingNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "MissingNode.instance (Jackson representation of a missing value)"
            },
            {
              "parameter": "format",
              "value": "MockOutputFormat returning \"MISSING_OK\""
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the supplied JsonNode is a Jackson MissingNode (which represents a missing property rather than a concrete JSON value), JsonSchema.validate should reject it and throw an IllegalArgumentException, because validation cannot be performed on a non‑existent node."
        }
      ]
    }
  ]
}