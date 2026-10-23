{
  "testCases": [
    {
      "group": "OpenAPI3StyleDiscriminators disabled - early success (errors empty, no type validator)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/properties/foo\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Config.isOpenAPI3StyleDiscriminators() = false, the schema list contains a single schema without a type validator, walkEnabled is false, schema.validate returns an empty Set. The validator should clear allErrors and return the empty Set immediately."
        }
      ]
    },
    {
      "group": "Type validator mismatch (schema type does not match node)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
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
              "value": "\"#/properties/bar\""
            }
          ],
          "expected": "Set containing a ValidationMessage with type = TYPE and message indicating expected type (e.g., \"integer\")",
          "description": "Config.isOpenAPI3StyleDiscriminators() = false, the only schema has a TypeValidator for INTEGER. The node is a STRING, so the validator adds a type‑mismatch message to allErrors and continues. After the loop there are no valid subschemas, so the method returns the collected type‑error."
        }
      ]
    },
    {
      "group": "OpenAPI3StyleDiscriminators enabled – discriminator match found with errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/0\""
            }
          ],
          "expected": "Set containing the schema validation errors plus a ValidationMessage with message ending in \"and the discriminator-selected candidate schema didn't pass validation\"",
          "description": "Config.isOpenAPI3StyleDiscriminators() = true, discriminatorContext.isDiscriminatorMatchFound() = true, schema.validate returns a non‑empty Set. The validator adds those errors, appends the DISCRIMINATOR_REMARK, clears no errors and returns the combined Set."
        }
      ]
    },
    {
      "group": "OpenAPI3StyleDiscriminators enabled – discriminator match found without errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf/1\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Config.isOpenAPI3StyleDiscriminators() = true, discriminatorContext.isDiscriminatorMatchFound() = true, schema.validate returns an empty Set. The validator clears allErrors and returns the empty Set (no remark is added)."
        }
      ]
    },
    {
      "group": "After loop – valid subschema clears required errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Config.isOpenAPI3StyleDiscriminators() = false, two schemas are present: the first fails with a REQUIRED‑type error, the second validates successfully (errors empty and state.hasMatchedNode() true). numberOfValidSubSchemas becomes 1 and childNotRequiredErrors is empty, so allErrors is replaced with the empty set and the method returns an empty Set."
        }
      ]
    },
    {
      "group": "After loop – discriminator active but no match found (fallback error)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=false)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf\""
            }
          ],
          "expected": "Set containing a single ValidationMessage with message \"based on the provided discriminator. No alternative could be chosen based on the discriminator property\"",
          "description": "Config.isOpenAPI3StyleDiscriminators() = true, discriminatorContext.isActive() = true, but discriminatorContext.isDiscriminatorMatchFound() = false for all subschemas. After the loop, numberOfValidSubSchemas = 0, so the fallback block creates the specific discriminator‑no‑alternative message and returns it."
        }
      ]
    },
    {
      "group": "Walk enabled path – schema.walk is used",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector, validatorState with hasMatchedNode=false, walkEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "at",
              "value": "\"#/anyOf\""
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "Config.isOpenAPI3StyleDiscriminators() = false, walkEnabled = true, the schema's walk method returns an empty Set. The validator treats it like the normal success case, increments numberOfValidSubSchemas, clears allErrors and returns the empty Set."
        }
      ]
    }
  ]
}