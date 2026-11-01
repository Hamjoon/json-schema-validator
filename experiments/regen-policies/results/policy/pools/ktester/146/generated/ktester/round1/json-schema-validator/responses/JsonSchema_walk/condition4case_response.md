{
  "testCases": [
    {
      "group": "Validate flag true",
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
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "When `validate` is true the method should walk the schema and perform validation, returning a successful ValidationResult for a node that satisfies the schema."
        },
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
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "ValidationResult.failure containing at least one ValidationMessage",
          "description": "When `validate` is true and the node does **not** conform to the schema, the walk should return a ValidationResult that contains validation errors."
        }
      ]
    },
    {
      "group": "Validate flag false",
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
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "ValidationResult.success (no validation messages) – walk performed without validation",
          "description": "When `validate` is false the method should still walk the schema but skip validation, therefore the returned ValidationResult must be successful regardless of node content."
        }
      ]
    },
    {
      "group": "Null executionCustomizer",
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
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "If the `executionCustomizer` argument is null the method must fall back to the default behaviour (no customisation) and still return a successful ValidationResult for a valid node."
        }
      ]
    },
    {
      "group": "Null executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": null
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "IllegalArgumentException with message \"ExecutionContext must not be null\"",
          "description": "The method is expected to guard against a null `ExecutionContext`. Passing null should cause an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Null node argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "IllegalArgumentException with message \"JsonNode must not be null\"",
          "description": "A null `JsonNode` is not acceptable for walking a schema; the method should throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer modifies context",
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
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionCustomizerThatAddsCustomAttribute"
            }
          ],
          "expected": "ValidationResult.success and the ExecutionContext contains the custom attribute set by the customizer",
          "description": "When a non‑null `ExecutionContextCustomizer` is supplied it must be invoked and allowed to mutate the context before validation proceeds."
        }
      ]
    }
  ]
}