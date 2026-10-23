{
  "testCases": [
    {
      "group": "Validator processing with mixed results – error and non‑error validators",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
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
          "expected": "Set containing the ValidationMessage produced by the ErrorValidator. No evaluated items or properties are added to the collector because the error validator is not of type PrefixItems/Items/Contains/Properties/AdditionalProperties/PatternProperties. The non‑error validator’s scope is merged into the parent scope.",
          "description": "Two validators are present: one (ErrorValidator) returns a non‑empty error set, the other (NoOpValidator) returns an empty set. The method must aggregate the error, skip adding evaluated items/properties for the error validator, and still merge the dynamic scope of the successful validator."
        }
      ]
    },
    {
      "group": "Validator processing without errors – merging dynamic scopes",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext (initially empty), config.isOpenAPI3StyleDiscriminators() = false }"
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
          "expected": "Empty Set. MockCollectorContext.evaluatedItems contains the items collected from the child scope of the first validator, and evaluatedProperties contains the properties collected from the child scope of the second validator, because both validators returned empty error sets and their scopes were merged into the parent scope.",
          "description": "All validators return empty error sets. The method must merge each validator’s dynamic scope into the parent scope, resulting in the collector context accumulating evaluated items and properties from all validators."
        }
      ]
    },
    {
      "group": "Validator processing with errors – non‑collection validator types",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = false }"
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
          "expected": "Set containing the ValidationMessage produced by the MaxLengthValidator. MockCollectorContext.evaluatedItems and evaluatedProperties remain unchanged because MaxLengthValidator is not one of the validator types that trigger collection of evaluated items or properties.",
          "description": "A validator that produces an error but is not an instance of PrefixItemsValidator, ItemsValidator, ItemsValidator202012, ContainsValidator, PropertiesValidator, AdditionalPropertiesValidator, or PatternPropertiesValidator. The method must add the error to the result set but must not copy any evaluated items or properties into the collector context."
        }
      ]
    },
    {
      "group": "No validators present – only discriminator handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() = MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"petType\" = \"cat\""
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
          "expected": "Empty Set. `discriminatorContext.registerDiscriminator` is called (if not already registered) and `checkDiscriminatorMatch` is invoked with the discriminator property value \"cat\". No validator‑related processing occurs because the validator map is empty.",
          "description": "The schema contains a discriminator object, the feature flag is enabled, but the JsonSchema instance has no validators (e.g., an empty schema). The method should skip the validator loop and still execute the discriminator logic."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – schema without discriminator node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() = MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"type\" = \"dog\""
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
          "expected": "Empty Set. Discriminator block is bypassed because `schemaNode.get(\"discriminator\")` returns null.",
          "description": "Even though the discriminator feature flag is on and a discriminator context exists, the schema itself does not define a discriminator object. The method must skip all discriminator processing."
        }
      ]
    },
    {
      "group": "OpenAPI discriminator handling – discriminator propertyName missing",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext = MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() = MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with property \"type\" = \"rabbit\""
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
          "expected": "Throws NullPointerException (or a descriptive ValidationException) because the discriminator object does not contain the required \"propertyName\" field, leading to a null result when calling `asText()`.",
          "description": "The discriminator object exists but is malformed (missing the mandatory \"propertyName\" field). The method attempts to read `discriminatorToUse.get(\"propertyName\").asText()` which results in a NPE. The test verifies that such schema mis‑configuration is not silently ignored."
        }
      ]
    }
  ]
}