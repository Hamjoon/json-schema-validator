{
  "testCases": [
    {
      "group": "Null root node",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            }
          ],
          "expected": "Empty Set – no validation messages (method should treat null as an empty JSON document and return no errors)",
          "description": "Validate that calling validate(null) does not throw an exception and returns an empty result set."
        }
      ]
    },
    {
      "group": "Required property validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            }
          ],
          "expected": "Empty Set – all required properties are present",
          "description": "Schema defines a required property \"name\". The JSON instance contains the property, so no ValidationMessage should be produced."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"required property 'name' is missing\"",
          "description": "Schema defines a required property \"name\". The JSON instance omits it, triggering the required‑validator branch and producing a validation error."
        }
      ]
    },
    {
      "group": "Type validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": 30 }"
            }
          ],
          "expected": "Empty Set – the value matches the expected type (integer)",
          "description": "Schema defines property \"age\" with type \"integer\". The JSON instance provides an integer, exercising the successful type‑validator path."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": \"thirty\" }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"type mismatch: expected integer but found string\"",
          "description": "Schema defines property \"age\" with type \"integer\". The JSON instance provides a string, causing the type‑validator branch to fail and emit a message."
        }
      ]
    },
    {
      "group": "Combined required + type validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"Alice\", \"age\": 25 }"
            }
          ],
          "expected": "Empty Set – both required and type constraints are satisfied",
          "description": "Schema requires \"name\" (string) and \"age\" (integer). The instance satisfies both, exercising the happy‑path where no branch adds a message."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": \"twenty\" }"
            }
          ],
          "expected": "Set containing two ValidationMessages: (1) required property 'name' is missing, (2) type mismatch for 'age' (expected integer, found string)",
          "description": "Both the required‑validator and type‑validator branches are triggered because the required property is absent and the present property has the wrong type."
        }
      ]
    },
    {
      "group": "Schema with $ref (reference) resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"address\": { \"city\": \"NY\" } }"
            }
          ],
          "expected": "Empty Set – the referenced schema for \"address\" resolves correctly and validates successfully",
          "description": "The schema contains a $ref to an external definition for \"address\". The JSON instance matches that definition, causing the reference‑resolution branch to succeed."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"address\": { \"zip\": 12345 } }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"required property 'city' is missing in referenced schema 'address'\"",
          "description": "The $ref branch is exercised, but the instance fails the referenced schema's required property, producing an error."
        }
      ]
    },
    {
      "group": "Dynamic anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"cat\" }"
            }
          ],
          "expected": "Empty Set – dynamic anchor resolves to the correct subschema and validates",
          "description": "Schema uses a dynamic anchor (e.g., \"$dynamicAnchor\": \"pet\") that points to a subschema requiring \"type\" to be one of [\"cat\",\"dog\"]. The instance matches, testing the dynamic‑anchor branch."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"rock\" }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"enum mismatch: value 'rock' is not allowed\"",
          "description": "The same dynamic anchor is used, but the instance violates the enum constraint, exercising the failure path of the dynamic‑anchor branch."
        }
      ]
    },
    {
      "group": "Discriminator validation (polymorphic schemas)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"petType\": \"dog\", \"bark\": true }"
            }
          ],
          "expected": "Empty Set – discriminator selects the correct subschema and validation passes",
          "description": "Schema defines a discriminator on property \"petType\" with mapping to subschemas for \"dog\" and \"cat\". The instance selects \"dog\" and satisfies its subschema, testing the discriminator branch."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"petType\": \"cat\", \"bark\": true }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"property 'meow' is required for discriminator value 'cat'\"",
          "description": "Discriminator selects the \"cat\" subschema, but the instance provides a property belonging to the \"dog\" subschema, causing the discriminator‑validation branch to emit an error."
        }
      ]
    },
    {
      "group": "Schema with additionalProperties = false",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"known\": 1 }"
            }
          ],
          "expected": "Empty Set – only defined properties are present",
          "description": "Schema disallows additional properties. The instance contains only the defined property, exercising the branch that skips additional‑property checks."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"known\": 1, \"extra\": \"value\" }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"additional property 'extra' is not allowed\"",
          "description": "The same schema encounters an unexpected property, triggering the additional‑properties‑false branch."
        }
      ]
    }
  ]
}