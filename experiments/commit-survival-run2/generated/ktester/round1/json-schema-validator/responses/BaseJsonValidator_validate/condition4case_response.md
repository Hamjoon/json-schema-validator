{
  "testCases": [
    {
      "group": "Null Input Tests",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Both parameters are null – the method should fail fast and throw a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "The JSON node argument is null – the validator must throw a NullPointerException before any processing."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            }
          ],
          "expected": "NullPointerException",
          "description": "The execution context is null – the method should throw a NullPointerException."
        }
      ]
    },
    {
      "group": "Schema Presence Tests",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When the validator's `schemaNode` field is null (no schema defined), the method should return an empty set of validation messages."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "objectNodeWithValidData"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A valid JSON object that conforms to a simple schema (type: object, no required properties). The validator should return an empty validation message set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingRequired"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='required'",
          "description": "The JSON object is missing a required property defined in the schema. The validator must report a `required` validation message."
        }
      ]
    },
    {
      "group": "Suppress Sub‑Schema Retrieval",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validNode"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "`suppressSubSchemaRetrieval` is true, the validator must skip calling `fetchSubSchemaNode` and only validate the current node. With a matching schema the result is an empty set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "invalidNode"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='type'",
          "description": "`suppressSubSchemaRetrieval` is true but the node type does not match the schema type (e.g., schema expects `object` but node is an `array`). The validator should report a type error without attempting sub‑schema validation."
        }
      ]
    },
    {
      "group": "Discriminator Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithDiscriminator"
            },
            {
              "parameter": "node",
              "value": "objectNodeWithMatchingDiscriminator"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The schema contains a `discriminator` object and the instance provides a matching discriminator property value. The static method `checkDiscriminatorMatch` should succeed and no validation messages are produced."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithDiscriminator"
            },
            {
              "parameter": "node",
              "value": "objectNodeWithMismatchingDiscriminator"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='discriminator'",
          "description": "The discriminator property value in the instance does not match any of the mapped schemas. The validator must generate a `discriminator` validation message."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithDiscriminator"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingDiscriminator"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='required'",
          "description": "The discriminator property is required by the schema but is absent in the instance. The validator should report a `required` validation message."
        }
      ]
    },
    {
      "group": "Apply Defaults Strategy",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextApplyDefaults"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingOptionalWithDefault"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "`applyDefaultsStrategy` is set to `APPLY_DEFAULTS`. The validator should inject default values for missing optional properties and therefore return no validation errors."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNoDefaults"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingOptionalWithDefault"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "`applyDefaultsStrategy` is `DO_NOT_APPLY_DEFAULTS`. Missing optional properties with defaults do not cause validation errors, so the result is still an empty set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNoDefaults"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingRequired"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='required'",
          "description": "When defaults are not applied, a missing required property must be reported as a `required` validation error."
        }
      ]
    },
    {
      "group": "Path Construction Branches",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "arrayNodeIndex0"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When validating an array element, the internal call to `atPath(currentPath, index)` should correctly build the JSON‑Pointer path (e.g., `/items/0`). The test verifies that no exception is thrown and the path is used in any generated messages."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "objectNodeWithNestedProperty"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When validating a nested object property, the call to `atPath(currentPath, token)` must correctly concatenate the token (e.g., `/address/street`). The test checks that the generated message (if any) contains the expected path."
        }
      ]
    }
  ]
}