{
  "testCases": [
    {
      "group": "walk_ValidateTrue_ValidNode_NoCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JSON object that fully conforms to the schema (e.g., {\"name\":\"John\",\"age\":30})"
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
          "expected": "ValidationResult.success (no validation messages, isSuccess() == true)",
          "description": "When validation is enabled and the supplied node satisfies the schema, walk should return a successful ValidationResult with no error messages."
        }
      ]
    },
    {
      "group": "walk_ValidateTrue_InvalidNode_NoCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JSON object that violates the schema (e.g., missing required property \"name\")"
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
          "expected": "ValidationResult.failure (contains at least one ValidationMessage, isSuccess() == false)",
          "description": "When validation is enabled and the node does not meet the schema requirements, walk should return a ValidationResult containing the appropriate validation errors."
        }
      ]
    },
    {
      "group": "walk_ValidateFalse_NodeIrrelevant_NoCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "Any JSON node (valid or invalid) – e.g., {\"unexpected\":true}"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success (no validation performed, isSuccess() == true)",
          "description": "When the validate flag is false, walk must skip schema validation and return a successful ValidationResult regardless of the node's content."
        }
      ]
    },
    {
      "group": "walk_WithCustomizer_ModifiesContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext() (initially empty custom data map)"
            },
            {
              "parameter": "node",
              "value": "Valid JSON node according to the schema"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "Consumer that puts key \"customFlag\" with value true into the ExecutionContext"
            }
          ],
          "expected": "ValidationResult.success and ExecutionContext contains entry {\"customFlag\":true}",
          "description": "The consumer passed as executionCustomizer should be invoked before validation, allowing the test to verify that the context was modified as expected."
        }
      ]
    },
    {
      "group": "walk_NullExecutionContext_ShouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": null
            },
            {
              "parameter": "node",
              "value": "Any valid JSON node"
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
          "expected": "NullPointerException",
          "description": "The method expects a non‑null ExecutionContext; passing null must result in a NullPointerException."
        }
      ]
    },
    {
      "group": "walk_NullNode_ShouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext()"
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
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "A null JsonNode is not a valid argument; the method should throw NullPointerException."
        }
      ]
    },
    {
      "group": "walk_WithCustomizer_ThrowsException_ShouldPropagate",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContext created via schema.createExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "Valid JSON node"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "Consumer that throws IllegalStateException(\"customizer failure\")"
            }
          ],
          "expected": "IllegalStateException with message \"customizer failure\"",
          "description": "If the consumer supplied as executionCustomizer throws an exception, walk must let that exception propagate to the caller."
        }
      ]
    }
  ]
}