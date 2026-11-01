{
  "test_cases": [
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
              "parameter": "node",
              "value": "mock JsonNode (any non‑null instance)"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "The method dereferences executionContext (e.g., executionContext.isFailFast()) before any null‑check, so passing a null executionContext must raise a NullPointerException."
        }
      ]
    },
    {
      "group": "Null node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
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
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "When node is null the call to typeValidator.equalsToSchemaType(node) (or later schema.validate/walk) will dereference the argument, causing a NullPointerException."
        }
      ]
    },
    {
      "group": "Null schemas list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "NullPointerException",
          "description": "If the AnyOfValidator is constructed with a null schemas list, the for‑each loop (`for (JsonSchema schema : this.schemas)`) will throw a NullPointerException."
        }
      ]
    },
    {
      "group": "TypeValidator throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "RuntimeException",
          "description": "A schema whose TypeValidator implementation throws a RuntimeException inside `equalsToSchemaType` or `validate`. The AnyOfValidator does not catch this, so the exception should propagate."
        }
      ]
    },
    {
      "group": "Schema.validate throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled, walk=false)"
            },
            {
              "parameter": "node",
              "value": "mock JsonNode"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "mock JsonNodePath"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "RuntimeException",
          "description": "When walk is false the validator calls `schema.validate`. If that method throws a RuntimeException, the AnyOfValidator does not handle it, so the exception must bubble up."
        }
      ]
    }
  ]
}