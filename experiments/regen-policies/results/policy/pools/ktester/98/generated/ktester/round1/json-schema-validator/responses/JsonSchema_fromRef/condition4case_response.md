{
  "testCases": [
    {
      "group": "Basic successful creation",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "pathMock"
            }
          ],
          "expected": "A new JsonSchema instance is returned. Its getValidationContext() contains the same meta‑schema, jsonSchemaFactory, config, schemaReferences, schemaResources and dynamicAnchors as the parentSchemaMock. Its evaluationPath equals pathMock, evaluationParentSchema equals parentSchemaMock, and its recursiveAnchor flag equals the original schema’s recursiveAnchor (false in this case).",
          "description": "Verify that fromRef creates a new JsonSchema with the evaluation context copied from the parent and the supplied evaluation path."
        }
      ]
    },
    {
      "group": "Parent schema with recursiveAnchor = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaRecursiveTrueMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "pathMock"
            }
          ],
          "expected": "Returned JsonSchema has recursiveAnchor == false (the flag is taken from the *current* schema, not the parent). All other fields in the ValidationContext are still copied from the parent.",
          "description": "Ensure that the recursiveAnchor flag of the new schema is NOT taken from the parent but from the original schema on which fromRef is invoked."
        }
      ]
    },
    {
      "group": "Parent schema with custom config",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithCustomConfigMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "customPathMock"
            }
          ],
          "expected": "The ValidationContext inside the new JsonSchema contains the custom config object supplied by the parent schema.",
          "description": "Validate that the config object from the parent schema is propagated to the new ValidationContext."
        }
      ]
    },
    {
      "group": "Null refEvaluationPath",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": null
            }
          ],
          "expected": "A JsonSchema instance is still created; its evaluationPath field is null. No NullPointerException is thrown.",
          "description": "Check that a null evaluation path is accepted and stored as‑is."
        }
      ]
    },
    {
      "group": "Null refEvaluationParentSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": null
            },
            {
              "parameter": "refEvaluationPath",
              "value": "pathMock"
            }
          ],
          "expected": "Throws NullPointerException when trying to access refEvaluationParentSchema.validationContext. The method should not succeed with a null parent.",
          "description": "Confirm that a null parent schema leads to an exception because the method dereferences the parent to build the new ValidationContext."
        }
      ]
    },
    {
      "group": "Original schema id propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "pathMock"
            }
          ],
          "expected": "The returned JsonSchema has the same id value as the original schema on which fromRef was called.",
          "description": "Verify that the id field is copied from the current schema into the newly created schema."
        }
      ]
    },
    {
      "group": "Validators and typeValidator are reset",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "pathMock"
            }
          ],
          "expected": "The new JsonSchema has validators == null, validatorsLoaded == false, and typeValidator == null regardless of the state of the original schema.",
          "description": "Ensure that fromRef always resets validator related fields as indicated by the local variables before constructing the new JsonSchema."
        }
      ]
    }
  ]
}