{
  "test_cases": [
    {
      "group": "fromRef - validator state reset",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithValidatorsLoaded"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/items\")"
            }
          ],
          "expected": {
            "evaluationParentSchema": "same instance as refEvaluationParentSchema",
            "evaluationPath": "JsonNodePath.of(\"/items\")",
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null
          },
          "description": "Parent schema has validatorsLoaded == true and non‑null requiredValidator/typeValidator/validators. After fromRef the copy must have validatorsLoaded reset to false and all validator‑related fields cleared, regardless of the parent’s previous state."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithPartialValidatorState"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/properties/age\")"
            }
          ],
          "expected": {
            "evaluationParentSchema": "same instance as refEvaluationParentSchema",
            "evaluationPath": "JsonNodePath.of(\"/properties/age\")",
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null
          },
          "description": "Parent schema has only requiredValidator set (typeValidator null). fromRef must clear requiredValidator and leave typeValidator null, ensuring a clean slate for the new schema."
        }
      ]
    },
    {
      "group": "fromRef - deep copy of validationContext collections",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithReferencesAndResources"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/definitions/address\")"
            }
          ],
          "expected": {
            "validationContext.schemaReferences": "same content as parent but different object identity",
            "validationContext.schemaResources": "same content as parent but different object identity",
            "validationContext.dynamicAnchors": "same content as parent but different object identity"
          },
          "description": "Parent schema’s ValidationContext contains non‑empty maps for schemaReferences, schemaResources and a set for dynamicAnchors. The copy created by fromRef must contain shallow copies of these collections (identical entries) but the collection objects themselves must be distinct, so modifications to the copy do not affect the parent."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithMutableConfig"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/properties/email\")"
            }
          ],
          "expected": {
            "validationContext.config": "identical values to parent’s config",
            "postModification.parentConfigUnchanged": true,
            "postModification.copyConfigChanged": true
          },
          "description": "Parent schema uses a custom SchemaValidatorsConfig (e.g., typeLoose = true). After fromRef the copy’s ValidationContext.config must initially be equal to the parent’s config. Changing a property on the copy’s config must not alter the parent’s config, proving that the config object is a deep copy."
        }
      ]
    },
    {
      "group": "fromRef - handling complex JsonNodePath",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/properties/name~1details\")"
            }
          ],
          "expected": {
            "evaluationPath": "JsonNodePath.of(\"/properties/name~1details\")"
          },
          "description": "JsonNodePath may contain escaped characters (\"~1\" for \"/\"). Verify that fromRef stores the exact path object without decoding or alteration."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": {
            "evaluationPath": "JsonNodePath.empty()"
          },
          "description": "An empty JsonNodePath (representing the root of the referenced schema) should be accepted and stored unchanged in the copy."
        }
      ]
    },
    {
      "group": "fromRef - multiple independent copies",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/a\")"
            }
          ],
          "expected": {
            "firstCopy != secondCopy": true,
            "firstCopy.evaluationParentSchema == secondCopy.evaluationParentSchema": true,
            "firstCopy.validationContext != secondCopy.validationContext": true
          },
          "description": "Calling fromRef twice on the same parent schema with the same path must produce two distinct JsonSchema instances. Both copies must reference the same parent schema instance, but each must have its own ValidationContext (different object identity)."
        },
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/b\")"
            }
          ],
          "expected": {
            "modifyingFirstCopy.validationContext.config": "does not affect secondCopy.validationContext.config"
          },
          "description": "After obtaining two copies, mutate the config of the first copy (e.g., enable failFast). Verify that the second copy’s config remains unchanged, confirming complete isolation."
        }
      ]
    },
    {
      "group": "fromRef - evaluationParentSchema reference integrity",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNestedParent"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.of(\"/nested\")"
            }
          ],
          "expected": {
            "evaluationParentSchema": "exact same instance passed as argument, not parentSchemaWithNestedParent.evaluationParentSchema"
          },
          "description": "Parent schema itself may have an evaluationParentSchema set (e.g., from a previous $ref). fromRef must set the copy’s evaluationParentSchema to the supplied parent schema instance, not to the parent’s own evaluationParentSchema."
        }
      ]
    }
  ]
}