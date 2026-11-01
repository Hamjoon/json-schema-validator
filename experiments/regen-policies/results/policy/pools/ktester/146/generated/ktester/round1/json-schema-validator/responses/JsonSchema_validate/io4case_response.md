{
  "newTestCases": [
    {
      "group": "Multiple validators – mixed error sets",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with discriminator disabled, Results mock that records setResult calls, and getValidators() stubbed to return three validators: v1 returns a non‑empty Set<ValidationMessage>, v2 returns null, v3 returns an empty Set"
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
          "expected": "The method creates a SetView, unions the non‑empty result from v1, ignores null/empty results from v2 and v3, returns a Set containing the messages from v1, and calls executionContext.getResults().setResult(instanceLocation, schemaLocation, evaluationPath, false).",
          "description": "Ensures that when several validators are present, the method correctly aggregates only the non‑empty error sets and still marks the instance as failed."
        }
      ]
    },
    {
      "group": "Discriminator enabled – missing discriminator property in instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where:\n  - getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true\n  - getCurrentDiscriminatorContext() returns a mock DiscriminatorContext\n  - DiscriminatorContext.getDiscriminatorForPath(schemaLocation) returns null\n  - DiscriminatorContext.registerDiscriminator(schemaLocation, discriminatorNode) is verified\n  - checkDiscriminatorMatch is verified (expects discriminatorPropertyValue null)"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"other\":\"value\"}"  ,
              "note": "does NOT contain the property defined by the discriminator (e.g., \"type\")"
            },
            {
              "parameter": "rootNode",
              "value": "{\"other\":\"value\"}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "registerDiscriminator is invoked with the discriminator defined in the schema; discriminatorToUse equals the schema discriminator; discriminatorNode is null, so discriminatorPropertyValue is null; checkDiscriminatorMatch is called with null value; no NullPointerException is thrown.",
          "description": "Verifies that when the discriminator property is absent in the instance, the method still calls checkDiscriminatorMatch with a null value rather than failing."
        }
      ]
    },
    {
      "group": "Discriminator enabled – jsonNode is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true and getCurrentDiscriminatorContext() returns a mock DiscriminatorContext"
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
              "value": "JsonNodePath.ROOT"
            }
          ],
          "expected": "The method throws a NullPointerException when attempting jsonNode.get(discriminatorPropertyName) because jsonNode is null.",
          "description": "Confirms the method’s behaviour when the supplied instance node is null – it should propagate the NPE (the method does not guard against null jsonNode)."
        }
      ]
    },
    {
      "group": "Validator throws RuntimeException – propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with discriminator disabled and getValidators() returning a single validator that throws a RuntimeException when validate() is called"
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
          "expected": "The RuntimeException propagates out of JsonSchema.validate; no further processing (error aggregation or setResult) occurs.",
          "description": "Ensures that the method does not swallow unchecked exceptions from sub‑validators."
        }
      ]
    }
  ]
}