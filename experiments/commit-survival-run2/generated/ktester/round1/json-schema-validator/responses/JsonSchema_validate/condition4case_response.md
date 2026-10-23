{
  "testCases": [
    {
      "group": "Validator processing without errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode representing valid JSON payload"
            },
            {
              "parameter": "rootNode",
              "value": "Same as jsonNode (no separate root needed for this test)"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set (no ValidationMessage). No merge of evaluated items/properties because all validators return empty set.",
          "description": "All validators return an empty result set, config disables discriminator handling. The method should iterate over validators, merge the dynamic scope and finally return an empty error set."
        }
      ]
    },
    {
      "group": "Validator processing with errors – PrefixItems/Items/Contains",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Set containing the ValidationMessage produced by the PrefixItemsValidator. MockCollectorContext.evaluatedItems contains the items collected from the validator’s scope.",
          "description": "A validator of type PrefixItemsValidator returns a non‑empty error set. The method must add those errors to the final result and copy the evaluated items from the child scope into the collector context."
        }
      ]
    },
    {
      "group": "Validator processing with errors – Properties/AdditionalProperties/PatternProperties",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Set containing the ValidationMessage produced by the PropertiesValidator. MockCollectorContext.evaluatedProperties contains the properties collected from the validator’s scope.",
          "description": "A validator of type PropertiesValidator returns a non‑empty error set. The method must add those errors to the final result and copy the evaluated properties from the child scope into the collector context."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – feature disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set (no ValidationMessage). The discriminator block is completely skipped.",
          "description": "When the configuration flag `isOpenAPI3StyleDiscriminators` is false the method must bypass all discriminator logic regardless of the schema content."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – discriminator node missing",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode without property \"type\""
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set. `checkDiscriminatorMatch` is called with discriminatorPropertyValue = null.",
          "description": "The schema contains a discriminator object, the configuration enables it, and a discriminator context exists, but the instance being validated does not contain the discriminator property. The method must still invoke `checkDiscriminatorMatch` with a null value."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – no discriminator context",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns null }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"type\" = \"cat\""
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set. Discriminator block is skipped because discriminatorContext is null.",
          "description": "Even though the schema defines a discriminator and the flag is enabled, the current discriminator context is missing, so the method must not attempt any discriminator matching."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – register new discriminator (no prior registration)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"petType\" = \"dog\""
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set. `discriminatorContext.registerDiscriminator(schemaPath, discriminatorNode)` is invoked once. `checkDiscriminatorMatch` receives the newly registered discriminator.",
          "description": "The schema contains a discriminator object, the config enables it, a discriminator context exists, but there is no discriminator already registered for this schemaPath. The method must register the current discriminator and then perform the match check."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – use already registered discriminator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"petType\" = \"bird\""
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "Empty Set. `discriminatorContext.getDiscriminatorForPath(schemaPath)` returns a non‑null ObjectNode, so the method does NOT call registerDiscriminator. `checkDiscriminatorMatch` receives that existing discriminator.",
          "description": "A discriminator is already registered for the current schemaPath. The method should retrieve it, skip registration, and invoke `checkDiscriminatorMatch` with the retrieved discriminator."
        }
      ]
    }
  ]
}