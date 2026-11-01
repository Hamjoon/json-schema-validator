{
  "testCases": [
    {
      "group": "Discriminator absent (schema without discriminator)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
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
          "expected": "registerDiscriminator is never invoked; checkDiscriminatorMatch is never invoked; method returns Collections.emptySet()",
          "description": "The schema does not contain a \"discriminator\" property. Even though OpenAPI‑3 style discriminators are enabled and a discriminator context exists, the method must skip both the pre‑validation registration and the post‑validation matching blocks."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=null)"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"b\":2})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"b\":2})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "no call to registerDiscriminator; no call to checkDiscriminatorMatch; method returns empty set",
          "description": "Same as previous case but the execution context has no discriminator context – the method must still avoid any discriminator‑related calls."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"c\":3})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"c\":3})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "discriminator handling code is completely bypassed; method returns empty set",
          "description": "OpenAPI‑3 style discriminators are disabled; even if a discriminator context exists, the method must not attempt any registration or matching."
        }
      ]
    },
    {
      "group": "Empty validator list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext(), validators=[])"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"x\":10})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"x\":10})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "errors set is empty; collectorContext.mergeWith is never called because there are no child scopes",
          "description": "When the schema has no validators, the method should skip the validation loop and directly return an immutable empty set."
        }
      ]
    },
    {
      "group": "Multiple error‑producing validators accumulation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, collectorContext=mockCollectorContext(), validators=[mockItemsValidator(returnErrors=[msg1]), mockPropertiesValidator(returnErrors=[msg2])])"
            },
            {
              "parameter": "jsonNode",
              "value": "arrayNode([1,2,3])"
            },
            {
              "parameter": "rootNode",
              "value": "arrayNode([1,2,3])"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "errors set contains both msg1 and msg2; collectorContext.getEvaluatedItems() receives items from ItemsValidator; collectorContext.getEvaluatedProperties() receives properties from PropertiesValidator; parentScope.mergeWith is NOT called for either validator because both returned non‑empty results",
          "description": "Two different validators each return a non‑empty error set. The method must accumulate both messages, add evaluated items and properties to the collector, and avoid merging the parent scope for those validators."
        }
      ]
    },
    {
      "group": "Null instance node with discriminator present",
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
          "expected": "java.lang.NullPointerException is thrown when the method reaches jsonNode.get(discriminatorPropertyName)",
          "description": "The schema defines a discriminator, OpenAPI‑3 style is enabled, but the instance node is null. The method should fail with NPE at the point it tries to read the discriminator property."
        }
      ]
    },
    {
      "group": "Discriminator without propertyName field",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"any\":\"value\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"any\":\"value\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "checkDiscriminatorMatch is called with discriminatorPropertyValue = null (because jsonNode.get(\"null\") returns null) and discriminatorToUse being the schema discriminator node (which lacks propertyName)",
          "description": "The schema contains a discriminator object but does not define the required \"propertyName\" field. The method should still attempt matching, using the string \"null\" as the property name, resulting in a null property value."
        }
      ]
    },
    {
      "group": "OpenAPI3 disabled but discriminator present",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=false, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"alien\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"alien\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "neither registerDiscriminator nor checkDiscriminatorMatch is invoked; method proceeds only with validator execution and returns whatever errors the validators produce",
          "description": "Even though the schema defines a discriminator, the configuration disables OpenAPI‑3 style discriminators, so the method must completely ignore discriminator handling."
        }
      ]
    }
  ]
}