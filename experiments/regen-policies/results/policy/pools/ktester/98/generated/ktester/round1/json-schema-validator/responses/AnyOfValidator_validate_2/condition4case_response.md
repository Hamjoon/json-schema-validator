{
  "testCases": [
    {
      "group": "Discriminator disabled - successful validation (walk = true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=true, discriminator disabled, failFast flag restored after call"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode representing valid instance"
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
          "expected": "empty Set<ValidationMessage>",
          "description": "Discriminator feature is off, the single subschema validates without errors (walk path). The validator should return an empty error set."
        }
      ]
    },
    {
      "group": "Discriminator disabled - type mismatch causes schema to be skipped",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled"
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
          "expected": "Set<ValidationMessage> containing the TypeValidator error message",
          "description": "The subschema has a non‑UNION TypeValidator that does not match the node type. The validator should skip the subschema validation, collect the TypeValidator error via allErrors and finally return that error set."
        }
      ]
    },
    {
      "group": "Discriminator disabled - short‑circuit on first valid subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=true, discriminator disabled"
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
          "expected": "empty Set<ValidationMessage>",
          "description": "The first subschema validates with no errors, canShortCircuit() and canShortCircuit(executionContext) both return true, and discriminator is disabled. The validator should short‑circuit and return the empty error set immediately."
        }
      ]
    },
    {
      "group": "Discriminator enabled - match found with validation errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext.isDiscriminatorMatchFound()=true"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that fails the matched subschema"
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
          "expected": "Set<ValidationMessage> containing the subschema errors (no short‑circuit)",
          "description": "Discriminator is active and a match is found. The matched subschema produces errors. The validator must add a discriminator remark message to allErrors and return the subschema errors."
        }
      ]
    },
    {
      "group": "Discriminator enabled - match found with no validation errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext.isDiscriminatorMatchFound()=true"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that satisfies the matched subschema"
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
          "expected": "empty Set<ValidationMessage>",
          "description": "When discriminator is enabled and a match is found, but the subschema validates without errors, the validator clears allErrors and returns the empty set."
        }
      ]
    },
    {
      "group": "Discriminator enabled - no match after evaluating all subschemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext.isActive()=true, isDiscriminatorIgnore()=false, no match found"
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
          "expected": "Set<ValidationMessage> containing a single message with argument \"based on the provided discriminator. No alternative could be chosen based on the discriminator property\"",
          "description": "After iterating all subschemas with discriminator enabled, the context is still active but no discriminator match was found. The validator must return the specific singleton message about the discriminator alternative."
        }
      ]
    },
    {
      "group": "walk = false – schema.validate() is used",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode"
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
              "value": false
            }
          ],
          "expected": "Set<ValidationMessage> as returned by JsonSchema.validate() for the single subschema",
          "description": "When walk flag is false the validator must call schema.validate() instead of schema.walk(). The returned error set from validate() should be propagated."
        }
      ]
    },
    {
      "group": "Multiple subschemas – at least one valid, no short‑circuit",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator disabled, canShortCircuit()=false"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that is valid for the second subschema only"
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
          "expected": "empty Set<ValidationMessage>",
          "description": "The first subschema produces errors, the second validates successfully. Because short‑circuit is disabled, the loop continues, but after the loop numberOfValidSubSchemas >= 1, so the validator returns an empty set."
        }
      ]
    },
    {
      "group": "No subschemas – empty schemas list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "any JsonNode"
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
          "expected": "empty Set<ValidationMessage>",
          "description": "When the AnyOfValidator has an empty schemas list, numberOfValidSubSchemas stays 0 and allErrors remains null. According to the final return statement the validator should return an empty set."
        }
      ]
    }
  ]
}