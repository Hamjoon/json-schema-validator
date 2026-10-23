{
  "testCases": [
    {
      "group": "Fetch Sub‑Schema Retrieval (Enabled)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithRef"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingRefRequired"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='required' from the referenced sub‑schema",
          "description": "`suppressSubSchemaRetrieval` is false. The validator's `schemaNode` contains a `$ref` that resolves to a sub‑schema requiring property `id`. The instance object does not contain `id`. The validator must fetch the sub‑schema via `fetchSubSchemaNode`, apply its rules and return a `required` validation message."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNoRef"
            },
            {
              "parameter": "node",
              "value": "objectNodeValidAgainstCurrentSchema"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "`suppressSubSchemaRetrieval` is false but `fetchSubSchemaNode` returns `null` (no `$ref` present). The current schema only checks that the node is an object, which it is, so validation succeeds with no messages."
        }
      ]
    },
    {
      "group": "Enum Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextEnumSchema"
            },
            {
              "parameter": "node",
              "value": "textNode\"yellow\""
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='enum'",
          "description": "The validator's `schemaNode` defines an `enum` of [\"red\",\"green\",\"blue\"]. The instance node contains the string \"yellow\", which is not part of the enum. Validation must produce an `enum` error."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextEnumSchema"
            },
            {
              "parameter": "node",
              "value": "textNode\"green\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Same enum schema as above, but the instance node contains the allowed value \"green\". Validation should succeed with no messages."
        }
      ]
    },
    {
      "group": "Number Constraints",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNumberRange"
            },
            {
              "parameter": "node",
              "value": "numericNode-1"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='minimum'",
          "description": "The schema specifies `type: number`, `minimum: 0`, `exclusiveMaximum: 10`. The node value is -1, violating the `minimum` constraint. The validator must report a `minimum` validation message."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNumberRange"
            },
            {
              "parameter": "node",
              "value": "numericNode10"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='exclusiveMaximum'",
          "description": "Using the same numeric schema, the node value is 10 which equals the `exclusiveMaximum`. Because the maximum is exclusive, validation must fail with an `exclusiveMaximum` message."
        }
      ]
    },
    {
      "group": "Boolean Type Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextBooleanSchema"
            },
            {
              "parameter": "node",
              "value": "textNode\"true\""
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='type'",
          "description": "Schema expects a boolean (`type: boolean`). The instance node is a JSON string \"true\". The validator must detect the type mismatch and emit a `type` validation message."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextBooleanSchema"
            },
            {
              "parameter": "node",
              "value": "booleanNodetrue"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Same boolean schema, but the instance node is a proper JSON boolean `true`. Validation should succeed with no messages."
        }
      ]
    },
    {
      "group": "Null Value Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNullSchema"
            },
            {
              "parameter": "node",
              "value": "nullNode"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Schema defines `type: null`. The instance node is a JSON null. The validator must accept it and return an empty set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNullSchema"
            },
            {
              "parameter": "node",
              "value": "numericNode5"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='type'",
          "description": "Schema still expects `null`, but the instance node is a number. Validation must fail with a `type` error."
        }
      ]
    },
    {
      "group": "Complex Nested Object with Path Verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNestedRequired"
            },
            {
              "parameter": "node",
              "value": "objectNodeMissingDeepRequired"
            }
          ],
          "expected": "Set containing a ValidationMessage with messageId='required' and path '/level1/level2/missingProp'",
          "description": "The schema defines a nested object structure: `level1` → `level2` → required property `missingProp`. The instance object lacks `missingProp`. The validator must build the correct JSON‑Pointer path using successive `atPath` calls and report the `required` message with the full path `/level1/level2/missingProp`."
        }
      ]
    }
  ]
}