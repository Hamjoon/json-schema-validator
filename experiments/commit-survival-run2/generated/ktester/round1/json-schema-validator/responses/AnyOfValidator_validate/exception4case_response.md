{
  "test_cases": [
    {
      "group": "Null executionContext parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
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
          "expected": "NullPointerException",
          "description": "When executionContext is null the call executionContext.getCollectorContext() throws NPE."
        }
      ]
    },
    {
      "group": "CollectorContext returned null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (getCollectorContext() returns null)"
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
          "expected": "NullPointerException",
          "description": "executionContext.getCollectorContext() returns null; the subsequent call collectorContext.enterDynamicScope() triggers NPE."
        }
      ]
    },
    {
      "group": "ValidatorState missing in CollectorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (collectorContext returns mockCollector where get(ValidatorState.VALIDATOR_STATE_KEY) returns null)"
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
          "expected": "NullPointerException",
          "description": "CollectorContext provides no ValidatorState; casting to ValidatorState yields null and the first call state.hasMatchedNode() throws NPE."
        }
      ]
    },
    {
      "group": "ValidationContext is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (valid collector & state)"
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
          "expected": "NullPointerException",
          "description": "The AnyOfValidator instance has its protected field 'validationContext' set to null (via reflection). The first usage validationContext.getConfig() throws NPE."
        }
      ]
    },
    {
      "group": "Config object inside ValidationContext is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (valid collector & state)"
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
          "expected": "NullPointerException",
          "description": "validationContext is non‑null but its getConfig() method returns null. The subsequent call config.isOpenAPI3StyleDiscriminators() throws NPE."
        }
      ]
    },
    {
      "group": "DiscriminatorContext null while OpenAPI3StyleDiscriminators enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (valid collector & state)"
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
          "expected": "NullPointerException",
          "description": "validationContext.getConfig().isOpenAPI3StyleDiscriminators() returns true but the validator's discriminatorContext field is null. The call validationContext.enterDiscriminatorContext(this.discriminatorContext, at) throws NPE."
        }
      ]
    },
    {
      "group": "Schema.validate returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (valid collector & state, walkEnabled=false)"
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
              "value": "\"#/anyOf/invalidValidate\""
            }
          ],
          "expected": "NullPointerException",
          "description": "A subschema mock is added to AnyOfValidator.schemas where schema.validate(executionContext, node, rootNode, at) returns null. The next line errors.isEmpty() triggers NPE."
        }
      ]
    },
    {
      "group": "Schema.walk returns null when walkEnabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext (valid collector & state, walkEnabled=true)"
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
              "value": "\"#/anyOf/invalidWalk\""
            }
          ],
          "expected": "NullPointerException",
          "description": "A subschema mock is added where schema.walk(...) returns null. The validator then calls errors.isEmpty() causing NPE."
        }
      ]
    }
  ]
}