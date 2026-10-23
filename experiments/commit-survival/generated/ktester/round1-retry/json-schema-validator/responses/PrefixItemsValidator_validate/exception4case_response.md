{
  "testCases": [
    {
      "group": "nullExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "[]"
            },
            {
              "parameter": "rootNode",
              "value": "[]"
            },
            {
              "parameter": "at",
              "value": "/"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the supplied ExecutionContext reference is null, the validator attempts to call executionContext.getCollectorContext() and throws a NullPointerException."
        }
      ]
    },
    {
      "group": "nullCollectorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext where getCollectorContext() returns null"
            },
            {
              "parameter": "node",
              "value": "[]"
            },
            {
              "parameter": "rootNode",
              "value": "[]"
            },
            {
              "parameter": "at",
              "value": "/"
            }
          ],
          "expected": "NullPointerException",
          "description": "If ExecutionContext.getCollectorContext() returns null, the subsequent call to getEvaluatedItems() triggers a NullPointerException."
        }
      ]
    },
    {
      "group": "nullEvaluatedItems",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with CollectorContext whose getEvaluatedItems() returns null"
            },
            {
              "parameter": "node",
              "value": "[1]"
            },
            {
              "parameter": "rootNode",
              "value": "[1]"
            },
            {
              "parameter": "at",
              "value": "/array"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the CollectorContext provides a null collection for evaluated items, the validator tries to add a path to it and throws a NullPointerException."
        }
      ]
    },
    {
      "group": "nullTupleSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty CollectorContext"
            },
            {
              "parameter": "node",
              "value": "[\"any\"]"
            },
            {
              "parameter": "rootNode",
              "value": "[\"any\"]"
            },
            {
              "parameter": "at",
              "value": "/field"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the PrefixItemsValidator instance has its internal tupleSchema field set to null, calling tupleSchema.size() causes a NullPointerException."
        }
      ]
    },
    {
      "group": "nullTupleSchemaElement",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty CollectorContext"
            },
            {
              "parameter": "node",
              "value": "[42]"
            },
            {
              "parameter": "rootNode",
              "value": "[42]"
            },
            {
              "parameter": "at",
              "value": "/value"
            }
          ],
          "expected": "NullPointerException",
          "description": "When tupleSchema contains a null entry at index 0, the validator attempts to invoke validate() on that null reference, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "tupleSchemaValidateThrowsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty CollectorContext"
            },
            {
              "parameter": "node",
              "value": "[\"trigger\"]"
            },
            {
              "parameter": "rootNode",
              "value": "[\"trigger\"]"
            },
            {
              "parameter": "at",
              "value": "/prop"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If a JsonSchema element inside tupleSchema throws an unchecked exception (e.g., IllegalStateException) during its validate() call, the PrefixItemsValidator does not catch it and propagates the exception."
        }
      ]
    },
    {
      "group": "nullAtParameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty CollectorContext"
            },
            {
              "parameter": "node",
              "value": "[true]"
            },
            {
              "parameter": "rootNode",
              "value": "[true]"
            },
            {
              "parameter": "at",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the 'at' argument is null, the internal atPath(at, i) helper attempts to concatenate a null base path and throws a NullPointerException."
        }
      ]
    }
  ]
}