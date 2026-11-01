{
  "test_cases": [
    {
      "group": "Pre‑validation discriminator cast exception (non‑ObjectNode)",
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
          "expected": "java.lang.ClassCastException is thrown when casting the discriminator node to ObjectNode",
          "description": "The schema contains a \"discriminator\" property whose value is an array node (or any non‑object node). During the pre‑validation block the code casts the node to ObjectNode, which must raise a ClassCastException."
        }
      ]
    },
    {
      "group": "Post‑validation discriminator cast exception (non‑ObjectNode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "java.lang.ClassCastException is thrown when casting the discriminator node to ObjectNode in the post‑validation block",
          "description": "The schema defines a \"discriminator\" whose value is an array node. After the validator loop the method attempts to cast it to ObjectNode for matching, which must fail with ClassCastException."
        }
      ]
    },
    {
      "group": "Null ValidationContext config",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(validationContext=mockValidationContext(config=null))"
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
          "expected": "java.lang.NullPointerException is thrown when calling isOpenAPI3StyleDiscriminators() on a null config",
          "description": "The ValidationContext returns null for getConfig(). The first line of the method dereferences the config, causing NPE."
        }
      ]
    },
    {
      "group": "Null CollectorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectorContext=null, validationContext=mockValidationContext(config=defaultConfig()))"
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
          "expected": "java.lang.NullPointerException is thrown when invoking enterDynamicScope on a null CollectorContext",
          "description": "The ExecutionContext returns null for getCollectorContext(). The loop tries to call collectorContext.enterDynamicScope(this) and fails."
        }
      ]
    },
    {
      "group": "Null validators list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(validationContext=mockValidationContext(config=defaultConfig()), collectorContext=mockCollectorContext())"
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
          "expected": "java.lang.NullPointerException is thrown when the for‑each loop attempts to iterate over a null validators list",
          "description": "The JsonSchema instance is constructed with getValidators() returning null. The method's for‑each over getValidators() triggers NPE."
        }
      ]
    },
    {
      "group": "Validator throws unchecked exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(validationContext=mockValidationContext(config=defaultConfig()), collectorContext=mockCollectorContext())"
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
          "expected": "java.lang.RuntimeException (or subclass) propagates out of validate()",
          "description": "One of the validators in the list throws a RuntimeException during its validate() call. The method does not catch it, so the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "CollectorContext returns null scope",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(validationContext=mockValidationContext(config=defaultConfig()), collectorContext=mockCollectorContext(exitScope=null))"
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
          "expected": "java.lang.NullPointerException is thrown when the method accesses scope.getEvaluatedItems() or scope.getEvaluatedProperties() after a validator returns a non‑empty error set",
          "description": "The collectorContext.exitDynamicScope() returns null. The validator returns a non‑empty Set, causing the method to attempt scope.getEvaluatedItems(), which triggers NPE."
        }
      ]
    },
    {
      "group": "Missing discriminator propertyName field (NPE)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "java.lang.NullPointerException is thrown when calling asText() on a null discriminator propertyName node",
          "description": "The schema contains a discriminator object but does not define the required \"propertyName\" field. In the post‑validation block discriminatorToUse.get(\"propertyName\") returns null, and the subsequent asText() call throws NPE."
        }
      ]
    },
    {
      "group": "Null schemaNode (NPE on discriminator access)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(openAPI3=true, currentDiscriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            }
          ],
          "expected": "java.lang.NullPointerException is thrown when attempting schemaNode.get(\"discriminator\") because schemaNode is null",
          "description": "The JsonSchema instance is constructed with a null schemaNode field. Both the pre‑validation and post‑validation discriminator handling code dereference schemaNode, causing NPE."
        }
      ]
    }
  ]
}