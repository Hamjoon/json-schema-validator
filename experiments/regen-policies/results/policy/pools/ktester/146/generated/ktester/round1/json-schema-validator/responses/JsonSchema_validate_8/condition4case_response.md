{
  "testCases": [
    {
      "group": "Null Parameter Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the root JSON node is null the method should immediately throw a NullPointerException."
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
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the OutputFormat argument is null the method should throw a NullPointerException before any validation occurs."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the execution customizer (Consumer) is null the method should throw a NullPointerException."
        }
      ]
    },
    {
      "group": "Successful Validation (no errors)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "A JSON document that fully conforms to the schema should produce an empty collection of ValidationMessage objects; the CollectingFormat returns an empty list."
        }
      ]
    },
    {
      "group": "Validation Failure (error messages produced)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "invalidJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[\"type mismatch\", \"required property missing\"]",
          "description": "A JSON document that violates the schema (e.g., wrong type, missing required property) should result in a non‑empty set of ValidationMessage objects; the CollectingFormat returns a list containing those messages."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Interaction",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new ContextAwareFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.put(\"customFlag\", true)"
            }
          ],
          "expected": "[\"customFlag=true\"]",
          "description": "The consumer should be invoked before validation runs. The customizer stores a flag in the ExecutionContext; ContextAwareFormat reads that flag and includes it in the returned result, proving the customizer was executed."
        }
      ]
    },
    {
      "group": "Recursive Anchor Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "simpleJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "When the schema is marked as a recursive anchor (isRecursiveAnchor() returns true) the validator must avoid infinite recursion. Supplying a trivial JSON node should complete normally and return an empty result."
        }
      ]
    },
    {
      "group": "Schema Resource Root Flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "new CollectingFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "[]",
          "description": "If the schema reports isSchemaResourceRoot() == true, the validator may resolve external $ref resources differently. The test verifies that with a valid document the method still returns an empty result, confirming the flag does not cause an error path."
        }
      ]
    }
  ]
}