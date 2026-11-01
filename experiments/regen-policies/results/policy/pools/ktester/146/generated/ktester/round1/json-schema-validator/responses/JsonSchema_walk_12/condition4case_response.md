{
  "testCases": [
    {
      "group": "Validate true with default OutputFormat (collect messages)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set of ValidationMessage containing no errors (empty set)",
          "description": "When validation is enabled and the node conforms to the schema, the walk method should return an empty set of validation messages via the collecting output format."
        }
      ]
    },
    {
      "group": "Validate true with default OutputFormat (collect messages) – failing node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "invalidJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set of ValidationMessage containing at least one error describing the schema violation",
          "description": "When validation is enabled and the node violates the schema, the walk method should return a non‑empty set of validation messages."
        }
      ]
    },
    {
      "group": "Validate false – no validation should be performed",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "anyJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set of ValidationMessage is empty (no validation performed)",
          "description": "When the validate flag is false, walk should bypass all validators and return an empty result regardless of node content."
        }
      ]
    },
    {
      "group": "Custom ExecutionContextCustomizer modifies the context",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "baseExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "Set of ValidationMessage is empty and the ExecutionContext used inside walk contains attribute \"custom\" set to true",
          "description": "The provided ExecutionContextCustomizer must be invoked before validation, allowing callers to enrich the context."
        }
      ]
    },
    {
      "group": "Null ExecutionContext – should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "The walk method expects a non‑null ExecutionContext; passing null must result in NPE."
        }
      ]
    },
    {
      "group": "Null JsonNode – should throw NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null JsonNode cannot be walked; the method should fail fast with NPE."
        }
      ]
    },
    {
      "group": "OutputFormat that throws a RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Object>() { public Object format(Set<ValidationMessage> messages) { throw new IllegalStateException(\"format failure\"); } }"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalStateException with message \"format failure\"",
          "description": "If the supplied OutputFormat throws an exception while formatting the validation messages, the walk method should propagate that exception."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer that throws an exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "outputFormat",
              "value": "new CollectingOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failure\"",
          "description": "Any exception thrown by the ExecutionContextCustomizer must be propagated to the caller."
        }
      ]
    }
  ]
}