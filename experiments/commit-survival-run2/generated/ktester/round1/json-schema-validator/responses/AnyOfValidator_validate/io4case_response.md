{
  "groups": [
    {
      "group": "Union type validator bypass – successful validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false, config.isOpenAPI3StyleDiscriminators()=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode (type STRING)"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/union\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The schema in the anyOf list has a TypeValidator whose schemaType is UNION. The method should skip the type‑mismatch check, invoke schema.validate (which returns an empty Set), clear allErrors and return the empty Set immediately (discriminators disabled)."
        }
      ]
    },
    {
      "group": "Type validator match – early success",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false, config.isOpenAPI3StyleDiscriminators()=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode (type INTEGER)"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/intSchema\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The only subschema has a TypeValidator for INTEGER and the node is an INTEGER. The type matches, schema.validate returns an empty Set, so the validator clears allErrors and returns the empty Set immediately (discriminators disabled)."
        }
      ]
    },
    {
      "group": "Valid subschema but matchedNode stays false – no valid count",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false, config.isOpenAPI3StyleDiscriminators()=false, state.setMatchedNode never called by schema)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode (type STRING)"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/noMatchFlag\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "The subschema validates successfully (returns empty Set) but does NOT set the validatorState.matchedNode flag. The method sees errors.isEmpty() and then `if (!state.hasMatchedNode()) continue;` so the subschema is **not** counted as a valid one. After the loop there are no errors, therefore the method returns an empty Set and finally sets state.matchedNode to true."
        }
      ]
    },
    {
      "group": "Valid subschema plus non‑required error from another schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false, config.isOpenAPI3StyleDiscriminators()=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode (type STRING)"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/mixed\""
            }
          ],
          "expected": "Set containing a single ValidationMessage of type TYPE (non‑required error)",
          "description": "Two subschemas are present. The first subschema validates successfully (empty errors, matchedNode set to true) and therefore increments numberOfValidSubSchemas. The second subschema fails with a TYPE‑mismatch error (e.g., expects INTEGER). After the loop, `childNotRequiredErrors` is not empty, so the validator does **not** clear the collected errors. The returned Set therefore contains the TYPE error from the failing subschema."
        }
      ]
    },
    {
      "group": "Walk enabled with schema.walk returning errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=true, config.isOpenAPI3StyleDiscriminators()=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode (type OBJECT)"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/walkError\""
            }
          ],
          "expected": "Set containing the ValidationMessage(s) produced by schema.walk",
          "description": "Because `state.isWalkEnabled()` is true, the validator calls `schema.walk` instead of `schema.validate`. The mocked schema returns a non‑empty Set (e.g., a REQUIRED‑property error). Since discriminators are disabled, the validator adds these errors to `allErrors` and continues. No subschema validates successfully, so after the loop the method returns the collected walk errors."
        }
      ]
    }
  ]
}