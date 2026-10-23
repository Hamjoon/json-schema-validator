{
  "testCases": [
    {
      "group": "walk_whenApplyDefaultsStrategyIsNull_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode([1,2,3])"
            },
            {
              "parameter": "rootNode",
              "value": "ArrayNode([1,2,3])"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator’s applyDefaultsStrategy field is deliberately set to null. When walk() checks applyDefaultsStrategy.shouldApplyArrayDefaults(), a NullPointerException must be thrown."
        }
      ]
    },
    {
      "group": "walk_whenTupleSchemaIsNull_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode([\"a\",\"b\"])"
            },
            {
              "parameter": "rootNode",
              "value": "ArrayNode([\"a\",\"b\"])"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator’s tupleSchema list is set to null. The loop condition calls this.tupleSchema.size(), which must raise a NullPointerException."
        }
      ]
    },
    {
      "group": "walk_whenTupleSchemaContainsNullElement_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode([1,2])"
            },
            {
              "parameter": "rootNode",
              "value": "ArrayNode([1,2])"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "tupleSchema contains a null entry at index 0. When walk() executes this.tupleSchema.get(0).getSchemaNode(), a NullPointerException must be thrown."
        }
      ]
    },
    {
      "group": "walk_whenTupleSchemaElementHasNullSchemaNode_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ArrayNode([null, 5])"
            },
            {
              "parameter": "rootNode",
              "value": "ArrayNode([null, 5])"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "A JsonSchema object is present in tupleSchema, but its getSchemaNode() method returns null. The subsequent call to .get(\"default\") on a null reference must raise a NullPointerException."
        }
      ]
    },
    {
      "group": "walk_whenNodeIsNull_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
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
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The supplied node argument is null. The first call inside walk() is node.isArray(), which must throw a NullPointerException."
        }
      ]
    },
    {
      "group": "walk_whenNodeClaimsArrayButIsNotArrayNode_shouldThrowClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "mockJsonNodeReturningTrueForIsArray"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNodeReturningTrueForIsArray"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ClassCastException",
          "description": "A custom JsonNode mock is configured so that node.isArray() returns true but the instance is not an ArrayNode. The cast (ArrayNode) node therefore triggers a ClassCastException."
        }
      ]
    },
    {
      "group": "walk_whenExecutionContextIsNull_andDoWalkUsesIt_shouldThrowNullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "ArrayNode([1])"
            },
            {
              "parameter": "rootNode",
              "value": "ArrayNode([1])"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator’s applyDefaultsStrategy is set to apply defaults, so walk() will invoke doWalk(). If doWalk() dereferences the passed executionContext (as the real implementation does), a NullPointerException must be observed."
        }
      ]
    }
  ]
}