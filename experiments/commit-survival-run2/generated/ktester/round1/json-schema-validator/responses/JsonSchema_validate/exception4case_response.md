{
  "testCases": [
    {
      "group": "Null executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode (any valid node)"
            },
            {
              "parameter": "rootNode",
              "value": "MockJsonNode (same as jsonNode)"
            },
            {
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "NullPointerException (thrown when setValidatorState or executionContext.getCollectorContext() is invoked)",
          "description": "The method receives a null ExecutionContext; any attempt to use it should result in an NPE."
        }
      ]
    },
    {
      "group": "Null collectorContext in executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { getCollectorContext() returns null }"
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
          "expected": "NullPointerException (thrown when collectorContext.enterDynamicScope(this) is called)",
          "description": "ExecutionContext is present but its collectorContext is null; the method should throw NPE during scope handling."
        }
      ]
    },
    {
      "group": "Null validationContext / config",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext }"
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
          "expected": "NullPointerException (thrown when this.validationContext.getConfig() is accessed)",
          "description": "The JsonSchema instance has a null validationContext; accessing its config causes NPE before any validator processing."
        }
      ]
    },
    {
      "group": "Null schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with discriminator property \"type\" = \"cat\""
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
          "expected": "NullPointerException (thrown at this.schemaNode.get(\"discriminator\"))",
          "description": "When the schemaNode field of JsonSchema is null, attempting to read the discriminator node results in NPE."
        }
      ]
    },
    {
      "group": "Discriminator node not an ObjectNode (ClassCastException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
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
          "expected": "ClassCastException (thrown when casting a non‑ObjectNode discriminator node to ObjectNode)",
          "description": "The schema contains a \"discriminator\" field whose value is an array or primitive instead of an object; the cast to ObjectNode fails."
        }
      ]
    },
    {
      "group": "Null jsonNode during discriminator processing",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
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
              "parameter": "at",
              "value": "\"$\""
            }
          ],
          "expected": "NullPointerException (thrown when jsonNode.get(discriminatorPropertyName) is invoked)",
          "description": "Even though the discriminator feature is enabled and a discriminator object exists, a null instance node causes NPE while trying to read the discriminator property."
        }
      ]
    },
    {
      "group": "Discriminator propertyName node is null (already covered but ensure explicit NPE)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "MockExecutionContext { collectorContext returns MockCollectorContext, config.isOpenAPI3StyleDiscriminators() = true, validationContext.getCurrentDiscriminatorContext() returns MockDiscriminatorContext }"
            },
            {
              "parameter": "jsonNode",
              "value": "MockJsonNode with any content"
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
          "expected": "NullPointerException (thrown when discriminatorToUse.get(\"propertyName\") returns null and .asText() is called)",
          "description": "The discriminator object exists but lacks the mandatory \"propertyName\" field, leading to NPE during property name extraction."
        }
      ]
    }
  ]
}