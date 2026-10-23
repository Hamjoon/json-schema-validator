{
  "testCases": [
    {
      "group": "Property node present - walk disabled, simple validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=false, walkEnabled=false, validationEnabled=true}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode with field \"name\" present"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (not used in this branch)"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Set containing ValidationMessage(s) returned from propertySchema.validate call (simulated as [\"error1\"]). The CollectorContext.evaluatedProperties should contain \"#/name\". No walkSchema invocation.",
          "description": "Verifies the path where the property exists, walk is disabled, and the validator is not complex. The child schema is validated directly and its errors are propagated."
        }
      ]
    },
    {
      "group": "Property node present - walk enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=false, walkEnabled=true, validationEnabled=true}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode with field \"age\" present"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Empty Set (walkSchema handles validation). The walkSchema method should be invoked once with the entry for \"age\".",
          "description": "Ensures that when walk is enabled the validator delegates to walkSchema instead of directly calling the child schema's validate method."
        }
      ]
    },
    {
      "group": "Property node present - complex validator handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=true, walkEnabled=false, validationEnabled=true, matchedNode initially false}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode with field \"type\" present"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Set containing ValidationMessage(s) from child validation. After validation, ValidatorState.matchedNode should be true (set before and after the recursive call). The original complex flag (true) must be restored.",
          "description": "Tests the logic that marks the node as matched before and after validation when the parent validator is complex (e.g., oneOf)."
        }
      ]
    },
    {
      "group": "Property node absent - required validator returns no errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=false}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode without field \"requiredField\""
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Empty Set (no required errors). The parent schema must have a RequiredValidator that returns an empty set.",
          "description": "Validates that when a property is missing but the required validator reports no errors, the validator returns an empty error set."
        }
      ]
    },
    {
      "group": "Property node absent - required validator returns errors, non‑complex validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=false}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode without field \"mandatory\""
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Set containing the required ValidationMessage(s) (e.g., [\"'mandatory' is required\"]). Since the validator is not complex, errors are added to the result set.",
          "description": "Ensures that missing mandatory properties generate required‑validation errors when the validator is not part of a complex construct."
        }
      ]
    },
    {
      "group": "Property node absent - required validator returns errors, complex validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState {complexValidator=true}"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode without field \"choice\""
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Empty Set (method returns early). ValidatorState.matchedNode should be set to false before returning.",
          "description": "Checks the early‑exit behavior when a required property is missing inside a complex validator (e.g., oneOf). The validator should clear errors and indicate the node did not match."
        }
      ]
    }
  ]
}