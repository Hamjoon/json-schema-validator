{
  "test_cases": [
    {
      "group": "Current schema with null ValidationContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "validParentSchemaMock"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPathMock"
            }
          ],
          "expected": "Throws NullPointerException because this.getValidationContext() returns null and the method attempts to call getMetaSchema() on it.",
          "description": "Validate that a JsonSchema instance whose internal ValidationContext is null cannot create a new schema via fromRef and raises NPE."
        }
      ]
    },
    {
      "group": "Parent schema with null ValidationContext field (direct field access)",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNullValidationContextField"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "somePath"
            }
          ],
          "expected": "Throws NullPointerException when the method accesses refEvaluationParentSchema.validationContext to retrieve config.",
          "description": "The parent schema's internal validationContext field is null (bypassing the getter). The method dereferences it directly, causing NPE."
        }
      ]
    },
    {
      "group": "Parent schema with null ValidationContext via getter",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithGetterReturningNull"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "path"
            }
          ],
          "expected": "Throws NullPointerException when the method calls refEvaluationParentSchema.getValidationContext().getConfig().",
          "description": "Even if the field exists, the getter returns null, leading to NPE on the subsequent call."
        }
      ]
    },
    {
      "group": "Parent ValidationContext with null config",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNullConfig"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException inside ValidationContext constructor when null config is passed (assuming constructor does not accept null).",
          "description": "The parent schema's ValidationContext returns null for getConfig(); the ValidationContext constructor is expected to reject null config."
        }
      ]
    },
    {
      "group": "Parent ValidationContext with null schemaReferences",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNullSchemaReferences"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) when ValidationContext constructor receives null schemaReferences.",
          "description": "Validate that missing schemaReferences in the parent ValidationContext cause an exception during new ValidationContext creation."
        }
      ]
    },
    {
      "group": "Parent ValidationContext with null schemaResources",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNullSchemaResources"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) when ValidationContext constructor receives null schemaResources.",
          "description": "Check that a null schemaResources map in the parent ValidationContext leads to an exception."
        }
      ]
    },
    {
      "group": "Parent ValidationContext with null dynamicAnchors",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "parentSchemaWithNullDynamicAnchors"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) when ValidationContext constructor receives null dynamicAnchors.",
          "description": "Ensure that a missing dynamicAnchors map in the parent ValidationContext triggers an exception."
        }
      ]
    },
    {
      "group": "Current schema with null meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "validParentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because this.getValidationContext().getMetaSchema() returns null and is passed to ValidationContext constructor.",
          "description": "Validate that the method cannot operate when the invoking schema's ValidationContext lacks a meta‑schema."
        }
      ]
    },
    {
      "group": "Current schema with null JsonSchemaFactory",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "validParentSchema"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "validPath"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because this.getValidationContext().getJsonSchemaFactory() returns null and is passed to ValidationContext constructor.",
          "description": "Ensure that a missing JsonSchemaFactory in the invoking schema's ValidationContext results in an exception."
        }
      ]
    }
  ]
}