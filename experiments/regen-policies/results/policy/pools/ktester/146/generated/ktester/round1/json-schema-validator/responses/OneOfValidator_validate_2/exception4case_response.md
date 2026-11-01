{
  "testCases": [
    {
      "group": "Null ExecutionContext – NPE on isFailFast",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "any valid JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "When executionContext is null the first call to executionContext.isFailFast() throws NPE."
        }
      ]
    },
    {
      "group": "Null node with discriminator enabled – NPE on node.get",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, failFast=true)"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "When discriminator is enabled the validator accesses node.get(propertyName). If node is null a NPE is thrown."
        }
      ]
    },
    {
      "group": "Null validationContext in OneOfValidator – NPE on getConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "any JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the OneOfValidator instance has its validationContext field set to null, the call this.validationContext.getConfig() throws NPE."
        }
      ]
    },
    {
      "group": "Parent schema missing – NPE on getValidators",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled)"
            },
            {
              "parameter": "node",
              "value": "any JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "When getParentSchema() returns null, the subsequent call .getValidators() results in NPE while trying to locate a discriminator validator."
        }
      ]
    },
    {
      "group": "Discriminator validator type mismatch – ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "ClassCastException",
          "description": "If a validator with keyword \"discriminator\" is present but is not an instance of DiscriminatorValidator, the cast ((DiscriminatorValidator) …) throws ClassCastException."
        }
      ]
    },
    {
      "group": "Discriminator property name null – NPE on isEmpty",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, DiscriminatorValidator.propertyName = null)"
            },
            {
              "parameter": "node",
              "value": "any JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "When discriminator.getPropertyName() returns null, the subsequent call .isEmpty() throws NPE."
        }
      ]
    },
    {
      "group": "Discriminator mapping null – NPE on getOrDefault",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled, DiscriminatorValidator.mapping = null)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "If discriminator.getMapping() returns null, the call .getOrDefault(...) throws NPE while trying to resolve the mapped value."
        }
      ]
    },
    {
      "group": "Schema node null – NPE on get(\"$ref\")",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator enabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "NullPointerException",
          "description": "If a child schema's getSchemaNode() returns null, the later call .get(\"$ref\") on a null object throws NPE."
        }
      ]
    },
    {
      "group": "enterDiscriminatorContext throws RuntimeException – propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext where enterDiscriminatorContext throws IllegalStateException"
            },
            {
              "parameter": "node",
              "value": "any JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "IllegalStateException",
          "description": "When discriminator is enabled the validator calls executionContext.enterDiscriminatorContext. If that method throws, the exception propagates out of validate()."
        }
      ]
    },
    {
      "group": "canShortCircuit overridden to throw – exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node that validates at least two schemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "RuntimeException",
          "description": "A subclass of OneOfValidator overrides canShortCircuit() to throw RuntimeException. The validate() method does not catch it, so the exception bubbles up."
        }
      ]
    },
    {
      "group": "reportChildErrors throws – exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (discriminator disabled) where reportChildErrors throws IllegalArgumentException"
            },
            {
              "parameter": "node",
              "value": "JSON node that fails all schemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When discriminator is disabled and a schema produces errors, validate() calls reportChildErrors(executionContext). If that method throws, the exception propagates."
        }
      ]
    }
  ]
}