{
  "groups": [
    {
      "group": "Pre‑walk listener allows validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mocked JsonNode (instance to validate)"
            },
            {
              "parameter": "rootNode",
              "value": "mocked JsonNode (root of the document)"
            },
            {
              "parameter": "instanceLocation",
              "value": "mocked JsonNodePath (path to the node)"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "errors set contains the ValidationMessage returned by the validator; pre‑walk listener invoked once with return true; post‑walk listener invoked once after validator execution.",
          "description": "The pre‑walk listener returns true, therefore the validator’s walk method is executed. The validator returns a non‑empty set of ValidationMessage objects which must be added to the final errors set. Post‑walk listener must be called regardless of the validator result."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mocked JsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "errors set is empty because validator returns an empty set; pre‑walk listener returns true; post‑walk listener still executed.",
          "description": "Pre‑walk listener allows the walk but the validator returns an empty Set. The method must not add any messages to the result, but it must still invoke the post‑walk listener."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mocked JsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "errors set is empty because validator returns null; pre‑walk listener returns true; post‑walk listener executed.",
          "description": "Validator’s walk method returns null. The code must guard against NPE and simply ignore the null result, leaving the errors set unchanged while still calling the post‑walk listener."
        }
      ]
    },
    {
      "group": "Pre‑walk listener skips validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mocked JsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "errors set is empty; validator.walk never invoked; post‑walk listener invoked once.",
          "description": "When the pre‑walk listener returns false, the walk must be skipped for that validator. No validator logic is executed, no errors are added, but the post‑walk listener must still be called."
        }
      ]
    },
    {
      "group": "Multiple validators with mixed outcomes",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mocked JsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mocked JsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "errors set contains messages from validator A (non‑empty) and validator C (non‑empty); validator B is skipped because its pre‑walk listener returns false; post‑walk listener called for all three validators.",
          "description": "Three validators are registered. Validator A and C allow the walk and return non‑empty error sets, which must be merged into the final result. Validator B’s pre‑walk listener returns false, so its walk is not executed and contributes no errors. Post‑walk listeners are always executed."
        }
      ]
    }
  ]
}