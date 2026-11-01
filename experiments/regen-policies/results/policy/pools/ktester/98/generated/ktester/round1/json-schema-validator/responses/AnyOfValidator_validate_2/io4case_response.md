{
  "tests": [
    {
      "group": "Union type validator does not skip on type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode of type STRING"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "Set<ValidationMessage> containing the validation errors produced by the subschema (type validator is UNION, so the schema is evaluated despite the node type mismatch)",
          "description": "The subschema’s TypeValidator reports JsonType.UNION. Even though the node is a STRING, the validator must not skip the subschema; it should invoke walk/validate and return whatever errors the subschema generates."
        }
      ]
    },
    {
      "group": "Short‑circuit disabled by execution context",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator disabled, canShortCircuit()=true, canShortCircuit(ExecutionContext)=false)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that satisfies the first subschema"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "empty Set<ValidationMessage> (validation succeeds) but the validator iterates over all remaining subschemas because the execution‑context short‑circuit check fails",
          "description": "canShortCircuit() returns true but canShortCircuit(executionContext) returns false, so the validator must not return early. It should still count the valid subschema, finish the loop, and finally return an empty set."
        }
      ]
    },
    {
      "group": "Discriminator enabled – ignore flag set",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, DiscriminatorContext.isDiscriminatorIgnore()=true, isDiscriminatorMatchFound()=false)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that fails the first subschema and passes the second"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "empty Set<ValidationMessage> (because the ignore flag makes the validator treat the discriminator as inactive and a valid subschema is found)",
          "description": "When the discriminator is enabled but the context signals to ignore it, the validator should behave as if the discriminator were disabled: it evaluates all subschemas, finds a valid one, clears allErrors and returns success."
        }
      ]
    },
    {
      "group": "FailFast flag restoration after validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (initial failFast=true, discriminator disabled). The mock records calls to setFailFast and getFailFast."
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that fails every subschema"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "Set<ValidationMessage> containing aggregated errors; after the call executionContext.getFailFast() returns true (original value restored)",
          "description": "The validator temporarily sets failFast to false, then restores the original value in the finally block. The test verifies that the original flag is unchanged after the method returns."
        }
      ]
    },
    {
      "group": "All subschemas invalid – aggregated errors returned",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode of type OBJECT that violates both subschemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "SetView<ValidationMessage> containing the union of errors from both subschemas (no valid subschema, numberOfValidSubSchemas == 0)",
          "description": "When every subschema produces at least one validation error, the validator must collect all errors into allErrors and return the combined set."
        }
      ]
    },
    {
      "group": "Multiple subschemas – first has type mismatch, second valid, short‑circuit disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled, canShortCircuit()=false)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode of type NUMBER"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "empty Set<ValidationMessage> (first subschema skipped due to type mismatch, second subschema validates successfully, short‑circuit disabled, final result empty)",
          "description": "The first subschema has a non‑UNION TypeValidator that does not match the node type, so it is skipped and its TypeValidator error is added to allErrors. The second subschema passes validation. Because short‑circuit is disabled, the loop continues, and the final return is an empty set (valid subschema found)."
        }
      ]
    }
  ]
}