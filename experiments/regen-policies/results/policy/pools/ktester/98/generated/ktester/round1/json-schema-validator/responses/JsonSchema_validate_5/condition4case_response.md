{
  "testCases": [
    {
      "group": "Null Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "mockFormat"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the JSON document (rootNode) is null the method should immediately throw a NullPointerException because validation cannot be performed on a non‑existent node."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the OutputFormat instance is null the method should reject the call with an IllegalArgumentException, as it has no way to produce a result."
        }
      ]
    },
    {
      "group": "Successful Validation (no messages)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatSuccess"
            }
          ],
          "expected": "outputSuccess",
          "description": "A JSON document that conforms to the schema should result in an empty set of ValidationMessage objects. The mock OutputFormat receives this empty set and returns a predefined success value (outputSuccess)."
        }
      ]
    },
    {
      "group": "Failed Validation (messages produced)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatCollect"
            }
          ],
          "expected": "outputFailure",
          "description": "A JSON document that violates the schema generates one or more ValidationMessage objects. The mock OutputFormat collects those messages and returns a predefined failure value (outputFailure)."
        }
      ]
    },
    {
      "group": "OutputFormat throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatThrows"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the OutputFormat implementation throws a RuntimeException while processing the validation result, the validate method should propagate that exception unchanged."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer influences validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockFormatSuccess"
            }
          ],
          "expected": "outputSuccessWithCustomizer",
          "description": "When a non‑null ExecutionContextCustomizer is supplied (via the overload that eventually calls this method), it may modify the ExecutionContext (e.g., enable/disable certain validators). The test verifies that the customizer is invoked and that validation still succeeds, returning the customizer‑aware output."
        }
      ]
    }
  ]
}