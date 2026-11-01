{
  "testCases": [
    {
      "group": "No schemas, OpenAPI3 discriminator disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_noSchemas_noDiscriminator"
            },
            {
              "parameter": "node",
              "value": "mockNode_any"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_any"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The validator has an empty schema list. No discriminator handling is enabled, so the method should skip the loop, not add any errors and finally return an empty set."
        }
      ]
    },
    {
      "group": "TypeValidator mismatch – schema ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_typeMismatch"
            },
            {
              "parameter": "node",
              "value": "mockNode_string"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_string"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_prop"
            }
          ],
          "expected": "Set containing one ValidationMessage produced by TypeValidator.validate",
          "description": "The schema has a non‑UNION TypeValidator that does not match the node type. The validator should call TypeValidator.validate, add its message to allErrors, continue to the next schema (there is only one), and finally return the collected error."
        }
      ]
    },
    {
      "group": "Valid subschema, walk disabled, OpenAPI3 disabled – early success",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_success_noWalk_noDiscriminator"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "State.isWalkEnabled() returns false, schema.validate() returns an empty set, and the validator state reports that a node has already been matched (state.hasMatchedNode() == true). Because OpenAPI3 discriminators are disabled, the method clears allErrors and returns the empty set immediately."
        }
      ]
    },
    {
      "group": "Valid subschema, walk enabled, OpenAPI3 disabled – early success after walk",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_success_walk_noDiscriminator"
            },
            {
              "parameter": "node",
              "value": "mockNode_array"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_array"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "State.isWalkEnabled() returns true, schema.walk() returns an empty set, state.hasMatchedNode() is true. With OpenAPI3 discriminators disabled the method clears allErrors and returns the empty set."
        }
      ]
    },
    {
      "group": "OpenAPI3 discriminator match found, errors present",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_errors_discriminatorMatch"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "Set containing original error(s) plus a DISCRIMINATOR_REMARK ValidationMessage",
          "description": "OpenAPI3 style discriminators are enabled and discriminatorContext.isDiscriminatorMatchFound() returns true. The subschema validation produces errors. The method must add those errors, append the discriminator remark, and return the (non‑empty) error set."
        }
      ]
    },
    {
      "group": "OpenAPI3 discriminator match found, no errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_success_discriminatorMatch"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "OpenAPI3 discriminators enabled, discriminator match found, but the subschema validates without errors. The method clears allErrors and returns an empty set."
        }
      ]
    },
    {
      "group": "After loop – required‑error filtering removes all non‑required errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_requiredFiltering"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Two subschemas are evaluated: one fails with a REQUIRED error, the other passes (no errors). numberOfValidSubSchemas >= 1 and after filtering out REQUIRED errors the remaining set is empty, so allErrors becomes empty. Because OpenAPI3 discriminators are disabled, the method returns the empty set."
        }
      ]
    },
    {
      "group": "After loop – required‑error filtering leaves non‑required errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_mixedErrors"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "Set containing the non‑required ValidationMessage(s)",
          "description": "One subschema returns a REQUIRED error, another returns a different error (e.g., MIN_LENGTH). numberOfValidSubSchemas is 0, so the method does not clear errors. After filtering, childNotRequiredErrors contains the non‑required error, which is kept in the final result."
        }
      ]
    },
    {
      "group": "OpenAPI3 discriminator active but no match – final discriminator message",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_noMatchActiveDiscriminator"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "Set containing a single ValidationMessage with text 'based on the provided discriminator. No alternative could be chosen based on the discriminator property'",
          "description": "OpenAPI3 discriminators are enabled and discriminatorContext.isActive() returns true, but no discriminator match was found during schema iteration. After the loop the method must return the specific 'no alternative could be chosen' message."
        }
      ]
    },
    {
      "group": "Final block – allErrors empty triggers state.setMatchedNode(true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_allSuccess"
            },
            {
              "parameter": "node",
              "value": "mockNode_object"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_object"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage> and verifier that executionContext.getValidatorState().setMatchedNode(true) was called once",
          "description": "All subschemas validate without errors, resulting in an empty allErrors set. The finally block must set the validator state matchedNode flag to true and merge the dynamic scopes."
        }
      ]
    }
  ]
}