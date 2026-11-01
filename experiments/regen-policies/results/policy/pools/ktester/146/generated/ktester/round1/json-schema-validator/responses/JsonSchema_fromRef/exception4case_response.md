{
  "testCases": [
    {
      "group": "Null config in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `config = null` (all other fields non‑null)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException if ValidationContext constructor validates) because `refEvaluationParentSchema.getValidationContext().getConfig()` is null and is dereferenced while constructing the new ValidationContext.",
          "description": "Ensures that a missing SchemaValidatorsConfig in the parent’s ValidationContext is detected and the method fails fast."
        }
      ]
    },
    {
      "group": "Null schemaReferences map in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `schemaReferences = null` (other fields valid)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because the constructor of ValidationContext receives a null `schemaReferences` map.",
          "description": "Verifies that the method does not silently accept a null map of schema references and throws an exception."
        }
      ]
    },
    {
      "group": "Null schemaResources map in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `schemaResources = null` (other fields valid)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because a null `schemaResources` map is passed to the new ValidationContext.",
          "description": "Confirms that missing schema resource mappings cause a failure rather than being silently ignored."
        }
      ]
    },
    {
      "group": "Null dynamicAnchors map in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `dynamicAnchors = null` (other fields valid)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because the ValidationContext constructor receives a null `dynamicAnchors` map.",
          "description": "Ensures that the method validates the presence of the dynamic anchors collection."
        }
      ]
    },
    {
      "group": "Null meta‑schema in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `metaSchema = null` (other fields valid)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because a null meta‑schema is supplied to the new ValidationContext.",
          "description": "Validates that the method does not accept a parent schema lacking a meta‑schema."
        }
      ]
    },
    {
      "group": "Null JsonSchemaFactory in parent ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal ValidationContext has `jsonSchemaFactory = null` (other fields valid)"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because the ValidationContext constructor receives a null `jsonSchemaFactory`.",
          "description": "Checks that the method fails fast when the factory required to create further schemas is missing."
        }
      ]
    }
  ]
}