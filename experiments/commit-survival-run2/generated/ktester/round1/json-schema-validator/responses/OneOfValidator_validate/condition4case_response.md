{
  "testCases": [
    {
      "group": "OneOfValidator.validate - walk disabled (state.isWalkEnabled == false)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "ValidationMessage set with numberOfValidSchema = 1, errors set empty, state.matchedNode == true",
          "description": "When walk is disabled the validator calls schema.validate(). The mocked schema returns an empty error set, causing numberOfValidSchema to become 1 and the method to return an empty error set."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "ValidationMessage set with numberOfValidSchema = 0, errors contain childErrors from the schema, collectorContext.evaluatedItems and evaluatedProperties cleared",
          "description": "When walk is disabled and the schema returns a non‑empty error set, numberOfValidSchema stays 0, the method adds the child errors to the result and clears evaluated items/properties."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - walk enabled (state.isWalkEnabled == true)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "ValidationMessage set with numberOfValidSchema = 1, errors empty, state.matchedNode == true",
          "description": "When walk is enabled the validator calls schema.walk(). The mocked schema returns an empty error set, causing a single valid schema and a successful validation."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "ValidationMessage set with numberOfValidSchema = 0, errors contain childErrors from the schema, collectorContext.evaluatedItems and evaluatedProperties cleared",
          "description": "When walk is enabled and the schema returns errors, numberOfValidSchema remains 0 and the method returns those errors."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - multiple valid schemas (short‑circuit)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "JsonSchemaException thrown if failFast==true, otherwise ValidationMessage with numberOfValidSchema = 2 and errors empty",
          "description": "Two schemas are added to the validator, both returning empty error sets. After the second schema the loop detects numberOfValidSchema > 1 and breaks. Since the final count is 2, the validator adds a OneOf error (or throws if failFast)."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - no valid schemas",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "ValidationMessage with numberOfValidSchema = 0 added to errors, childErrors from all schemas also present, collectorContext.evaluatedItems and evaluatedProperties cleared",
          "description": "All schemas return non‑empty error sets, leaving numberOfValidSchema at 0. The validator must produce a OneOf error (\"0\") together with the collected child errors."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - failFast true",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "JsonSchemaException thrown containing the OneOf ValidationMessage",
          "description": "When failFast flag on the OneOfValidator instance is true and the number of valid schemas is not exactly one, the method throws JsonSchemaException instead of returning an error set."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - state matchedNode handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true, matchedNode=false))" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "If state.hasMatchedNode() returns false after schema.validate(), the validator will ignore the empty schemaErrors and numberOfValidSchema stays 0, leading to a OneOf error",
          "description": "Tests the branch where schemaErrors.isEmpty() but state.hasMatchedNode() is false, causing the validator to continue without counting the schema as valid."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true, matchedNode=true))" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "at", "value": "\"/path\"" }
          ],
          "expected": "numberOfValidSchema increments to 1 and final errors set is empty; state.matchedNode set to true before return",
          "description": "Ensures that when state.hasMatchedNode() is true the validator counts the schema as valid."
        }
      ]
    }
  ]
}