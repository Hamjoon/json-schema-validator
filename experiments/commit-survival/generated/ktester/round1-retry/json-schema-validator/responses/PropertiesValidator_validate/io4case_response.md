{
  "testCases": [
    {
      "group": "No schemas defined",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing a default ValidatorState (complexValidator=false, walkEnabled=false, validationEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "any mock JsonNode (properties map is empty, so node.get(...) always returns null)"
            },
            {
              "parameter": "rootNode",
              "value": "any JsonNode (not used when schemas map is empty)"
            },
            {
              "parameter": "at",
              "value": "\"#\""
            }
          ],
          "expected": "Empty Set. Since the validator holds no property schemas, the loop body is never executed and no ValidationMessage is produced.",
          "description": "Verifies that when the PropertiesValidator has an empty `schemas` map the method returns an empty, immutable set without touching the CollectorContext or ValidatorState."
        }
      ]
    },
    {
      "group": "Parent schema without required validator, property absent",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing a default ValidatorState (complexValidator=false, walkEnabled=false, validationEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode that does NOT contain the field \"optional\""
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
          "expected": "Empty Set. The parent schema reports `hasRequiredValidator() == false`, therefore the missing property is ignored and no errors are added.",
          "description": "Ensures that when a property is missing but the parent schema does not define a RequiredValidator, the validator simply returns an empty error set."
        }
      ]
    },
    {
      "group": "Property node present - walk enabled with validation errors from walkSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState (complexValidator=false, walkEnabled=true, validationEnabled=true) and a stub WalkListenerRunner that records the call"
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
          "expected": "Set containing a ValidationMessage \"walkError\" that is added by the stubbed `walkSchema` implementation. No direct call to `propertySchema.validate` should occur.",
          "description": "Checks that when walk is enabled the validator delegates to `walkSchema`; the stubbed walkSchema injects an error, which must be returned to the caller."
        }
      ]
    },
    {
      "group": "Multiple properties - mixed present and absent with required validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState (complexValidator=false, walkEnabled=false, validationEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode containing field \"presentProp\" only"
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
          "expected": "Set containing two ValidationMessages: \"presentError\" (produced by the child schema for \"presentProp\") and \"'missingProp' is required\" (produced by the parent RequiredValidator for the absent property). The evaluatedProperties set should contain \"#/presentProp\" only.",
          "description": "Validates that the validator correctly processes several entries: it validates the existing property, adds its error, and for the missing property it invokes the RequiredValidator, adding its error to the final set."
        }
      ]
    },
    {
      "group": "Property node present - walk disabled, non‑complex validator, matchedNode unchanged",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext containing ValidatorState (complexValidator=false, walkEnabled=false, validationEnabled=true, matchedNode initially false)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode with field \"simple\" present"
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
          "expected": "Empty Set (child schema returns no errors). After the call `ValidatorState.matchedNode` remains false because the validator is not part of a complex construct.",
          "description": "Ensures that when the validator is not marked as complex, the `matchedNode` flag is never toggled, even though the property exists and validation is performed."
        }
      ]
    }
  ]
}