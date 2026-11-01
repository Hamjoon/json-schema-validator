{
  "testCases": [
    {
      "group": "Id propagation – parent schema id retained",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance with id=\"parent-schema-id\", default config, validationContext non‑null"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/properties/name\")"
            }
          ],
          "expected": "Returned JsonSchema is non‑null and its getId() equals \"parent-schema-id\". No exception is thrown.",
          "description": "Ensures that the identifier of the parent schema is copied to the schema created by fromRef."
        }
      ]
    },
    {
      "group": "Dynamic anchors propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose ValidationContext contains a dynamicAnchors map with entry {\"myAnchor\" -> some JsonSchema}"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"#/definitions/obj\")"
            }
          ],
          "expected": "Returned JsonSchema’s ValidationContext.getDynamicAnchors() contains the same entry {\"myAnchor\" -> same JsonSchema object}. No exception is thrown.",
          "description": "Verifies that dynamic anchors stored in the parent’s ValidationContext are correctly copied to the new schema."
        }
      ]
    },
    {
      "group": "Schema resources map propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose ValidationContext has a non‑empty schemaResources map (e.g., {\"http://example.com/schema\" -> JsonNode})"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"#/components/schemas/Item\")"
            }
          ],
          "expected": "Returned JsonSchema’s ValidationContext.getSchemaResources() is the same map instance (or contains the same entries). No exception is thrown.",
          "description": "Checks that schema resource references are propagated unchanged."
        }
      ]
    },
    {
      "group": "Evaluation path with URL‑encoded characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "Valid JsonSchema instance with default configuration"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/properties/%20name\")"
            }
          ],
          "expected": "Returned JsonSchema’s getEvaluationPath() equals the supplied JsonNodePath (the encoded segment is preserved). No exception is thrown.",
          "description": "Ensures that fromRef does not attempt to decode or otherwise alter an already URL‑encoded path."
        }
      ]
    },
    {
      "group": "Root evaluation path – empty JsonNodePath",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "Valid JsonSchema instance with default configuration"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "Returned JsonSchema’s evaluationPath is empty (represents the root of the schema). No exception is thrown.",
          "description": "Validates handling of a reference that points to the root of the schema."
        }
      ]
    },
    {
      "group": "Parent schema with null ValidationContext – defensive error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance whose internal validationContext field is deliberately set to null"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/any\")"
            }
          ],
          "expected": "Method throws NullPointerException (or IllegalArgumentException) because it dereferences parentSchema.validationContext.",
          "description": "Confirms that the method fails fast when the parent schema lacks a ValidationContext."
        }
      ]
    },
    {
      "group": "Returned schema is a distinct instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "JsonSchema instance with id=\"parent\", default config"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/properties/age\")"
            }
          ],
          "expected": "The object returned by fromRef is not the same reference as refEvaluationParentSchema (reference inequality). No exception is thrown.",
          "description": "Ensures that fromRef creates a new JsonSchema object rather than returning the parent."
        }
      ]
    },
    {
      "group": "Validators list remains null until explicit initialization",
      "cases": [
        {
          "input": [
            {
              "parameter": "refEvaluationParentSchema",
              "value": "Valid JsonSchema instance with default configuration"
            },
            {
              "parameter": "refEvaluationPath",
              "value": "new JsonNodePath(\"/properties/email\")"
            }
          ],
          "expected": "Immediately after fromRef, calling getValidators() on the returned schema returns null (or an empty list) because validators are not loaded yet. No exception is thrown.",
          "description": "Verifies that the internal validators collection is not pre‑populated; it will be loaded later via initializeValidators()."
        }
      ]
    }
  ]
}