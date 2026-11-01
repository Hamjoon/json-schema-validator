{
  "testCases": [
    {
      "group": "Invalid $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"$ref\":\"http://[\"})"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException – malformed URI in $ref causes parsing failure",
          "description": "When the instance contains a `$ref` with an invalid URI, the walk method should throw an IllegalArgumentException while trying to resolve the reference."
        }
      ]
    },
    {
      "group": "Missing node type handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "MissingNode.getInstance()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException – MissingNode cannot be processed as a regular JSON value",
          "description": "Passing a `MissingNode` (Jackson's representation of a non‑existent node) should cause the walk method to reject the input with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Uninitialized validators",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"name\":\"John\"})"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalStateException – schema validators have not been initialized before walking",
          "description": "If a `JsonSchema` instance is created but `initializeValidators()` is never called, invoking `walk` should fail with an IllegalStateException because the internal validator list is null."
        }
      ]
    },
    {
      "group": "Circular $ref without recursive anchor",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"self\":{\"$ref\":\"#\"}})"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "StackOverflowError – infinite recursion when resolving a circular $ref without a $recursiveAnchor",
          "description": "A schema that references itself via `$ref` but does not declare `$recursiveAnchor` should cause the walk method to recurse indefinitely, eventually throwing a StackOverflowError."
        }
      ]
    }
  ]
}