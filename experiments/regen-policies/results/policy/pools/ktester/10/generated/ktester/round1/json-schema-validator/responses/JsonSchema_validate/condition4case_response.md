{
  "testCases": [
    {
      "group": "OpenAPI3 style discriminator registration (pre‑validation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "discriminatorContext.registerDiscriminator(schemaLocation, discriminatorNode) is called exactly once",
          "description": "When the schema contains a non‑null \"discriminator\" object and the execution context reports OpenAPI‑3 style discriminators, the method must register the discriminator before any validator runs."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=null)"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "no call to registerDiscriminator; method proceeds without NPE",
          "description": "If the execution context has no current discriminator context, the early registration block must be skipped safely."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "registerDiscriminator is never invoked",
          "description": "When OpenAPI‑3 style discriminators are disabled, the method must not attempt any registration regardless of the schema content."
        }
      ]
    },
    {
      "group": "Validator result handling and collector merging",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath()"
            }
          ],
          "expected": "errors set is empty; collectorContext.mergeWith is called for each validator because each validator returns null or empty set",
          "description": "All validators return null (or empty) results; the method should merge the child scope back into the parent without adding any ValidationMessage."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath()"
            }
          ],
          "expected": "errors set contains the messages returned by the failing validator; collectorContext.getEvaluatedItems() receives items from the validator if it is an instance of ItemsValidator/PrefixItemsValidator/ItemsValidator202012/ContainsValidator; collectorContext.getEvaluatedProperties() receives properties from the validator if it is an instance of PropertiesValidator/AdditionalPropertiesValidator/PatternPropertiesValidator",
          "description": "A validator returns a non‑empty Set<ValidationMessage> and is of type ItemsValidator. The method must add those messages to the final error set and copy the evaluated items from the child scope to the collector."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath()"
            }
          ],
          "expected": "errors set contains messages from multiple validators; each validator’s scope is merged only when its result set is empty; otherwise parentScope.mergeWith is not called for that validator",
          "description": "Mix of validators – some return errors, others return none – to ensure the conditional merge logic works correctly for each iteration."
        }
      ]
    },
    {
      "group": "OpenAPI3 style discriminator matching (post‑validation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\": \"cat\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\": \"cat\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "checkDiscriminatorMatch is called with discriminatorPropertyValue = \"cat\" and discriminatorToUse = schema discriminator node",
          "description": "Schema contains a discriminator with propertyName \"type\". The instance provides that property with value \"cat\"; the method must resolve the discriminator (no prior registration) and invoke the matching check."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext(withRegisteredDiscriminator=discriminatorNodeFromAllOf))"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\": \"dog\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\": \"dog\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "discriminatorContext.getDiscriminatorForPath returns a non‑null node, so discriminatorToUse is that node; checkDiscriminatorMatch receives the value \"dog\"",
          "description": "When the discriminator has already been registered (e.g., via an allOf), the method must reuse the registered discriminator instead of the one defined on the current schema."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "discriminatorNode is null → discriminatorPropertyValue is null; checkDiscriminatorMatch is called with null value",
          "description": "The instance does not contain the discriminator property; the method must still call checkDiscriminatorMatch with a null property value."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=null)"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"bird\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"bird\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "no call to checkDiscriminatorMatch; method returns normally",
          "description": "If there is no discriminator context even though OpenAPI‑3 style is enabled, the discriminator matching block must be skipped safely."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"fish\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"fish\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "discriminator node exists but discriminatorFromContext is null, so the method registers the discriminator and then calls checkDiscriminatorMatch",
          "description": "Ensures the branch where discriminatorFromContext == null registers the current discriminator before matching."
        }
      ]
    },
    {
      "group": "No errors returned (empty set handling)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"a\":1})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"a\":1})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "method returns Collections.emptySet()",
          "description": "When no validator produces any ValidationMessage and OpenAPI‑3 discriminators are disabled, the method must return an immutable empty set."
        }
      ]
    }
  ]
}