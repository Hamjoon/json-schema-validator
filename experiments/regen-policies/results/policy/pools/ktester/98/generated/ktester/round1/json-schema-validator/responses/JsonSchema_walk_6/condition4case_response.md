Invalid control character at: line 14 column 62 (char 344)
{
  "groups": [
    {
      "group": "Basic walk delegation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode() // empty object"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with no errors (empty ValidationMessage set)",
          "description": "When a valid ExecutionContext, an empty JSON object and validate=true are supplied and no customizer is provided, walk should delegate to walkAtNodeInternal and return a successful ValidationResult."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode() // empty object"
            },
            {
              "parameter": "validate",
              "value": "false"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with no errors (empty ValidationMessage set)",
          "description": "When validate is false the schema should still walk the node but skip validation; the result must still be a valid ValidationResult with an empty error set."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer (Consumer) effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // contains a mutable map 'attributes'"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")"
            }
          ],
          "expected": "ValidationResult with no errors and executionContext.attributes['custom'] == \"value\"",
          "description": "A non‑null Consumer should be invoked before validation; the test verifies that the consumer can mutate the ExecutionContext and that the mutation is visible after walk returns."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failed\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failed\"",
          "description": "If the Consumer throws an exception, walk should propagate that exception (the method does not catch it). This covers the branch where executionCustomizer is invoked."
        }
      ]
    },
    {
      "group": "Null or invalid parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "walk expects a non‑null ExecutionContext; passing null should trigger a NPE (covers the null‑check branch)."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "walk expects a non‑null JsonNode; passing null should cause a NPE."
        }
      ]
    },
    {
      "group": "Interaction with walkAtNodeInternal (branch verification via spy)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "spy(new ExecutionContext())"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "walkAtNodeInternal is called exactly once with OutputFormat.RESULT and the same parameters",
          "description": "Using a Mockito spy on JsonSchema, verify that walk delegates to walkAtNodeInternal with the correct OutputFormat (RESULT) and passes through all arguments. This covers the internal conditional that chooses the overload with Consumer."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "spy(new ExecutionContext())"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {} // no‑op consumer"
            }
          ],
          "expected": "walkAtNodeInternal is called with the Consumer overload (not the ExecutionContextCustomizer overload)",
          "description": "When a Consumer is supplied, the method should call the private overload that accepts a Consumer, not the one that accepts ExecutionContextCustomizer. The spy verifies the correct overload is chosen."
        }
      ]
    }
  ]
}