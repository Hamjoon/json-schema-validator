{
  "testCases": [
    {
      "group": "fromRef - basic functionality",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithCustomConfig"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/properties/name\")"
            }
          ],
          "expected": {
            "evaluationParentSchema": "same instance as refEvaluationParentSchema",
            "evaluationPath": "JsonNodePath.of(\"/properties/name\")",
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null,
            "validationContext.config": "identical to parentSchemaWithCustomConfig.validationContext.config"
          },
          "description": "Verify that fromRef creates a new JsonSchema copy whose evaluationParentSchema and evaluationPath are set to the supplied arguments, all validator‑related state is reset, and the validationContext is rebuilt using the parent’s configuration."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNoValidatorsLoaded"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "null"
            }
          ],
          "expected": {
            "evaluationParentSchema": "same instance as refEvaluationParentSchema",
            "evaluationPath": null,
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null,
            "validationContext.config": "identical to parentSchemaWithNoValidatorsLoaded.validationContext.config"
          },
          "description": "When the supplied JsonNodePath is null the method should still create a copy; the copy’s evaluationPath should be null and no exception should be thrown."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "null"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/any\")"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null parent schema should cause a NullPointerException because the method dereferences the parent to obtain its validationContext."
        }
      ]
    },
    {
      "group": "fromRef - isolation of original schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "originalSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/sub\")"
            }
          ],
          "expected": {
            "originalSchema.evaluationParentSchema": "unchanged (null or previous value)",
            "originalSchema.evaluationPath": "unchanged",
            "originalSchema.validatorsLoaded": "unchanged",
            "originalSchema.requiredValidator": "unchanged",
            "originalSchema.typeValidator": "unchanged",
            "originalSchema.validators": "unchanged"
          },
          "description": "Ensure that invoking fromRef does not mutate any state of the original parent schema; only the newly created copy is modified."
        }
      ]
    }
  ]
}