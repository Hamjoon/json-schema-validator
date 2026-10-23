{
  "testCases": [
    {
      "group": "walk_whenApplyDefaultsStrategyIsDisabled_shouldReturnEmptyMessagesAndNotModifyArray",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([1,2,3])" },
            { "parameter": "rootNode", "value": "ArrayNode([1,2,3])" },
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "arrayContent": "[1,2,3]"
          },
          "description": "The validator’s ApplyDefaultsStrategy is set to NOT apply defaults. Even though the node is an array, walk should skip the default‑application loop and return an empty ValidationMessage set without mutating the array."
        }
      ]
    },
    {
      "group": "walk_whenNodeIsNotArray_shouldReturnEmptyMessagesAndIgnoreTupleSchema",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ObjectNode({\"a\":1})" },
            { "parameter": "rootNode", "value": "ObjectNode({\"a\":1})" },
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "nodeUnchanged": true
          },
          "description": "When the supplied node is not an array, the walk method must bypass the default‑application logic entirely, returning an empty ValidationMessage set and leaving the original node untouched."
        }
      ]
    },
    {
      "group": "walk_whenTupleSchemaIsEmpty_shouldIterateZeroTimes",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([\"a\",\"b\"])"},
            { "parameter": "rootNode", "value": "ArrayNode([\"a\",\"b\"])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "doWalkInvocations": 0
          },
          "description": "If the validator’s tupleSchema list is empty, the loop limit becomes min(node.size(),0) = 0, therefore doWalk is never invoked and no validation messages are produced."
        }
      ]
    },
    {
      "group": "walk_whenNodeSizeLessThanTupleSchemaSize_shouldIterateNodeSizeTimes",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([1,2])"},
            { "parameter": "rootNode", "value": "ArrayNode([1,2])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "doWalkInvocations": 2,
            "invokedIndices": [0,1]
          },
          "description": "When the input array has fewer elements than the tupleSchema length, walk must iterate only over the existing elements (i.e., node.size()), invoking doWalk for each index up to node.size()‑1."
        }
      ]
    },
    {
      "group": "walk_whenNodeSizeGreaterThanTupleSchemaSize_shouldIterateTupleSchemaSizeTimes",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([\"x\",\"y\",\"z\"])"},
            { "parameter": "rootNode", "value": "ArrayNode([\"x\",\"y\",\"z\"])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "doWalkInvocations": 2,
            "invokedIndices": [0,1]
          },
          "description": "If the array contains more items than the tupleSchema, walk must stop at tupleSchema.size() (the minimum of the two lengths), invoking doWalk only for the first tupleSchema.size() indices."
        }
      ]
    },
    {
      "group": "walk_whenElementIsNullAndDefaultExists_shouldReplaceWithDefaultAndPassDefaultToDoWalk",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([null, 5])"},
            { "parameter": "rootNode", "value": "ArrayNode([null, 5])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "arrayContentAfterWalk": "[\"defaultValue\",5]",
            "doWalkInvocations": 2,
            "doWalkReceivedNodeAtIndex0": "\"defaultValue\""
          },
          "description": "For an array element that is JSON null and whose corresponding tuple schema defines a \"default\" property, walk must replace the null with the default value, update the underlying ArrayNode, and invoke doWalk with the default node."
        }
      ]
    },
    {
      "group": "walk_whenElementIsNullAndNoDefault_shouldPassNullToDoWalkWithoutModification",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([null, \"b\"])"},
            { "parameter": "rootNode", "value": "ArrayNode([null, \"b\"])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "arrayContentAfterWalk": "[null,\"b\"]",
            "doWalkInvocations": 2,
            "doWalkReceivedNodeAtIndex0": "null"
          },
          "description": "When an element is null but the associated tuple schema does not provide a default, walk must leave the element unchanged and still call doWalk with the original null node."
        }
      ]
    },
    {
      "group": "walk_whenElementIsNonNull_shouldPassOriginalNodeToDoWalk",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "ArrayNode([10, null])"},
            { "parameter": "rootNode", "value": "ArrayNode([10, null])"},
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": {
            "validationMessages": "empty set",
            "arrayContentAfterWalk": "[10,null]",
            "doWalkInvocations": 2,
            "doWalkReceivedNodeAtIndex0": "10"
          },
          "description": "If an array element is already a concrete (non‑null) value, walk must forward that exact node to doWalk without attempting any default substitution."
        }
      ]
    }
  ]
}