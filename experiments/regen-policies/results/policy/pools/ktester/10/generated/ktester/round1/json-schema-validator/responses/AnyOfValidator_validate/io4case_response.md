{
  "new_test_cases": [
    {
      "group": "Union TypeValidator bypasses type check",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_unionType_noDiscriminator"
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
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The schema's TypeValidator reports JsonType.UNION. The validator must skip the type‑mismatch check, invoke the schema's validate() (which returns an empty set), clear allErrors and return an empty set. No TypeValidator.validate call should be made."
        }
      ]
    },
    {
      "group": "Schema without TypeValidator proceeds to validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_noTypeValidator_noDiscriminator"
            },
            {
              "parameter": "node",
              "value": "mockNode_number"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode_number"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockInstanceLocation_root"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The schema returns null from getTypeValidator(). The validator should directly call schema.validate() (which yields an empty set), treat the subschema as valid, clear any accumulated errors and return an empty set."
        }
      ]
    },
    {
      "group": "Valid subschema plus non‑required error from another schema – non‑required error retained",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_mixedValidAndError_noDiscriminator"
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
          "expected": "Set containing the MIN_LENGTH ValidationMessage from the second subschema",
          "description": "Two subschemas are present. The first validates successfully (state.hasMatchedNode() becomes true, numberOfValidSubSchemas increments). The second returns a non‑required error (e.g., MIN_LENGTH). After the loop, childNotRequiredErrors is not empty, therefore the validator must keep all accumulated errors. The final set should contain only the MIN_LENGTH error."
        }
      ]
    }
  ]
}