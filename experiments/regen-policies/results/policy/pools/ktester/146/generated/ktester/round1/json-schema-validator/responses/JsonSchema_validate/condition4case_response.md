{
  "testCases": [
    {
      "group": "Discriminator disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns false"
            },
            {
              "parameter": "jsonNode",
              "value": "any JsonNode (e.g., {} )"
            },
            {
              "parameter": "rootNode",
              "value": "same as jsonNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "any JsonNodePath (e.g., JsonNodePath.ROOT)"
            }
          ],
          "expected": "No call to ExecutionContext.getCurrentDiscriminatorContext() or registerDiscriminator; no call to checkDiscriminatorMatch; only validator results are returned.",
          "description": "Validate when the discriminator keyword feature is disabled – the method must skip all discriminator‑related logic."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator node missing",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true and getCurrentDiscriminatorContext() returns a mock context"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "rootNode",
              "value": "{}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "Since schemaNode.get(\"discriminator\") is null, no registration nor checkDiscriminatorMatch is performed. Errors are only those produced by validators.",
          "description": "Feature enabled but schema does not contain a discriminator definition – the method must bypass both registration and matching."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present, current context null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true and getCurrentDiscriminatorContext() returns null"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "rootNode",
              "value": "{}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "No call to registerDiscriminator (because current discriminator context is null). No checkDiscriminatorMatch is executed.",
          "description": "The discriminator keyword is present but there is no active DiscriminatorContext – the method must not attempt registration or matching."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present, context present, discriminatorFromContext null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where:\n  - getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true\n  - getCurrentDiscriminatorContext() returns mock DiscriminatorContext\n  - DiscriminatorContext.getDiscriminatorForPath(schemaLocation) returns null\n  - DiscriminatorContext.registerDiscriminator(schemaLocation, discriminatorNode) is verified\n  - checkDiscriminatorMatch is verified to be called with the current discriminator"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"car\"}"
            },
            {
              "parameter": "rootNode",
              "value": "{\"type\":\"car\"}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "registerDiscriminator is invoked with the discriminator defined in the schema; discriminatorToUse = schema discriminator; checkDiscriminatorMatch is called with the property value \"car\".",
          "description": "When the schema defines a discriminator and the current context has no entry for this schema location, the method must register the schema discriminator and then perform the match check."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present, context present, discriminatorFromContext non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where:\n  - getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true\n  - getCurrentDiscriminatorContext() returns mock DiscriminatorContext\n  - DiscriminatorContext.getDiscriminatorForPath(schemaLocation) returns a different ObjectNode (context discriminator)\n  - DiscriminatorContext.registerDiscriminator is NOT called\n  - checkDiscriminatorMatch is called with the context discriminator"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"bike\"}"
            },
            {
              "parameter": "rootNode",
              "value": "{\"type\":\"bike\"}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "No registration occurs; discriminatorToUse is the one retrieved from the context; checkDiscriminatorMatch is invoked with the value \"bike\".",
          "description": "When a discriminator is already registered for the schema location, the method must reuse it and skip registration."
        }
      ]
    },
    {
      "group": "Validator returns non‑empty error set",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with discriminator disabled (to isolate validator branch) and with Results mock that records setResult calls"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "rootNode",
              "value": "{}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "Validator.validate returns a non‑empty Set<ValidationMessage>; errors collection is created; after loop, errors is non‑empty, thus executionContext.getResults().setResult(instanceLocation, schemaLocation, evaluationPath, false) is invoked.",
          "description": "Ensures that when at least one sub‑validator produces errors, the method aggregates them and marks the instance location as failed."
        }
      ]
    },
    {
      "group": "Validator returns empty or null error set",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with discriminator disabled and Results mock"
            },
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "rootNode",
              "value": "{}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "All validators return null or empty sets; errors remains null; method returns Collections.emptySet(); executionContext.getResults().setResult is NOT called.",
          "description": "Verifies that when no validator reports a problem, the method returns an empty error set and does not mark the instance as failed."
        }
      ]
    }
  ]
}