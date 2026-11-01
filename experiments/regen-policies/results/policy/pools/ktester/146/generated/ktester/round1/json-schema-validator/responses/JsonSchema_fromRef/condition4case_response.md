{
  "testCases": [
    {
      "group": "Normal operation – typical reference resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance with default config, id=\"parent\", recursiveAnchor=false, validationContext having metaSchema=MetaSchema.V4, config=SchemaValidatorsConfig.DEFAULT"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/properties/name\")"
            }
          ],
          "expected": "A non‑null JsonSchema object is returned. Its `getValidationContext()` contains the same meta‑schema and config as the parent schema, its `getEvaluationPath()` equals the supplied JsonNodePath, and its `getEvaluationParentSchema()` equals the supplied parent schema. No exception is thrown.",
          "description": "Verifies that fromRef creates a new schema with the evaluation path and parent schema correctly propagated when all inputs are valid."
        }
      ]
    },
    {
      "group": "Parent schema with recursive anchor enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance where `recursiveAnchor` is true (set via `setRecursiveAnchor(true)`) and other fields default"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"#/definitions/address\")"
            }
          ],
          "expected": "Returned JsonSchema has `isRecursiveAnchor()` equal to true (inherited from the parent). All other fields are identical to the normal case. No exception is thrown.",
          "description": "Ensures that the recursive‑anchor flag is copied from the parent schema into the newly created schema."
        }
      ]
    },
    {
      "group": "Null evaluation path – fallback handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "Valid JsonSchema instance with default configuration"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "null"
            }
          ],
          "expected": "Returned JsonSchema has a `null` evaluation path (or the constructor substitutes `JsonNodePath.empty()` – whichever the implementation does). No NullPointerException is thrown.",
          "description": "Checks that passing a null JsonNodePath does not cause a failure and the new schema is still created."
        }
      ]
    },
    {
      "group": "Null parent schema – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "null"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException if the method validates arguments) because the method dereferences `refEvaluationParentSchema.validationContext`.",
          "description": "Validates that the method does not silently accept a null parent schema and fails fast."
        }
      ]
    },
    {
      "group": "Parent schema with custom ValidationContext configuration",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose ValidationContext has a custom `SchemaValidatorsConfig` (e.g., `typeLoose = true`) and a non‑empty `schemaReferences` map"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"#/definitions/custom\")"
            }
          ],
          "expected": "Returned JsonSchema’s ValidationContext contains the same custom config and the same schemaReferences map as the parent. All other fields are correctly propagated.",
          "description": "Ensures that complex objects inside ValidationContext (config, references, resources, dynamic anchors) are copied correctly."
        }
      ]
    },
    {
      "group": "Parent schema with suppressed sub‑schema retrieval flag set",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance created with `suppressSubSchemaRetrieval = true`"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"#/definitions/ignore\")"
            }
          ],
          "expected": "Returned JsonSchema has `suppressSubSchemaRetrieval` set to true (inherited from the parent). No exception is thrown.",
          "description": "Verifies that the boolean flag controlling sub‑schema retrieval is propagated to the new schema."
        }
      ]
    }
  ]
}