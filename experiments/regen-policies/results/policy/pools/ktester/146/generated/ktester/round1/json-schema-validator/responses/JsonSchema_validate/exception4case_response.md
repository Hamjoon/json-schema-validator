{
  "test_cases": [
    {
      "group": "Discriminator node not an ObjectNode – ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true and getCurrentDiscriminatorContext() returns a mock DiscriminatorContext"
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
          "expected": "ClassCastException thrown when casting schemaNode.get(\"discriminator\") (which is a text node) to ObjectNode.",
          "description": "The schema contains a \"discriminator\" keyword whose value is a JSON string instead of an object. The method casts it to ObjectNode, which should raise a ClassCastException."
        }
      ]
    },
    {
      "group": "Discriminator without propertyName – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns true, getCurrentDiscriminatorContext() returns a mock DiscriminatorContext, and DiscriminatorContext.getDiscriminatorForPath(schemaLocation) returns null"
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
          "expected": "NullPointerException thrown when calling discriminatorToUse.get(\"propertyName\").asText() because the discriminator object lacks a \"propertyName\" field.",
          "description": "The discriminator object is present but does not contain the required \"propertyName\" field. The subsequent call to .asText() on a null JsonNode should cause an NPE."
        }
      ]
    },
    {
      "group": "ExecutionContext with null ValidationContext – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext() returns null"
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
          "expected": "NullPointerException thrown when the method accesses executionContext.getValidationContext().getConfig().isDiscriminatorKeywordEnabled().",
          "description": "The ExecutionContext does not provide a ValidationContext; the first line of the method dereferences a null, causing an NPE."
        }
      ]
    },
    {
      "group": "ExecutionContext with null Config – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig() returns null"
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
          "expected": "NullPointerException thrown when calling isDiscriminatorKeywordEnabled() on a null Config object.",
          "description": "The ValidationContext is present but its Config is null, leading to an NPE on the feature‑flag check."
        }
      ]
    },
    {
      "group": "ExecutionContext with null Results – NullPointerException on setResult",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns false, getResults() returns null, and getValidators() returns a list with one validator that returns a non‑empty error set"
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
          "expected": "NullPointerException thrown when the method attempts executionContext.getResults().setResult(...).",
          "description": "When validators produce errors, the method tries to record the failure via Results; if Results is null, a NPE should be thrown."
        }
      ]
    },
    {
      "group": "JsonSchema.getValidators returns null – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns false"
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
          "expected": "NullPointerException thrown when the for‑each loop tries to iterate over a null collection returned by getValidators().",
          "description": "The JsonSchema instance is stubbed so that getValidators() returns null. The method does not guard against this and will throw an NPE during iteration."
        }
      ]
    },
    {
      "group": "JsonSchema.getValidators contains null validator – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where getValidationContext().getConfig().isDiscriminatorKeywordEnabled() returns false"
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
          "expected": "NullPointerException thrown when the loop invokes v.validate(...) on a null validator element.",
          "description": "The validator list contains a null entry. The method attempts to call validate on it, leading to an NPE."
        }
      ]
    }
  ]
}