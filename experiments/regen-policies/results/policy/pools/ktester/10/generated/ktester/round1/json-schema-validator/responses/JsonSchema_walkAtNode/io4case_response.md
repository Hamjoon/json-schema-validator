{
  "groups": [
    {
      "group": "walkAtNode - successful validation (validate = true)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeMatchingSchema" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = true and no messages",
          "description": "The node conforms to the schema (all required properties present, correct types). walkAtNode should return a successful ValidationResult when validate flag is true."
        }
      ]
    },
    {
      "group": "walkAtNode - successful validation (validate = false)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeMatchingSchema" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "false" }
          ],
          "expected": "ValidationResult with isValid = true and no messages (walk performed but validators skipped)",
          "description": "When validate flag is false the method should still walk the schema tree (allowing listeners to run) but must not apply validators, resulting in a valid result."
        }
      ]
    },
    {
      "group": "walkAtNode - type mismatch",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validArrayNode" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = false and a message containing \"type\" indicating expected object but got array",
          "description": "The schema expects an object (type validator present) but the instance node is an array. walkAtNode should report a type error."
        }
      ]
    },
    {
      "group": "walkAtNode - missing required property",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeMissingRequired" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = false and a message containing \"required\" for the missing property",
          "description": "Schema defines a required property that is absent in the instance. walkAtNode must invoke the required validator and return the appropriate error."
        }
      ]
    },
    {
      "group": "walkAtNode - $ref resolution (local fragment)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeReferencingLocalDef" },
            { "parameter": "rootNode", "value": "validRootNodeWithDefinitions" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = true (the $ref is resolved to a subschema that validates the node)",
          "description": "The schema contains a $ref to a local definition (e.g., \"#/definitions/positiveInteger\"). walkAtNode must resolve the reference using the provided rootNode and validate accordingly."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeReferencingMissingDef" },
            { "parameter": "rootNode", "value": "validRootNodeWithoutDefinitions" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = false and a message indicating unresolved $ref fragment",
          "description": "The $ref points to a non‑existent definition. walkAtNode should fail with a clear unresolved reference error."
        }
      ]
    },
    {
      "group": "walkAtNode - recursive anchor handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContextWithRecursiveAnchorEnabled" },
            { "parameter": "node", "value": "validObjectNodeRecursiveStructure" },
            { "parameter": "rootNode", "value": "validRootNodeRecursive" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = true (recursive anchor correctly prevents infinite loop)",
          "description": "Schema uses \"$recursiveAnchor\": true and a \"$recursiveRef\". walkAtNode must recognise the anchor and avoid endless recursion, returning a successful result for a valid recursive instance."
        }
      ]
    },
    {
      "group": "walkAtNode - keyword walk listener invocation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContextWithListener" },
            { "parameter": "node", "value": "validObjectNodeMatchingSchema" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = true and listener recorded entry/exit events for each keyword",
          "description": "When a WalkListenerRunner is attached to the schema, walkAtNode must call its `enterKeyword` and `exitKeyword` callbacks for each validator. The test verifies that the listener was invoked the expected number of times."
        }
      ]
    },
    {
      "group": "walkAtNode - malformed JsonNodePath",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNode" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "malformedInstanceLocation" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "ValidationResult with isValid = false and a message indicating invalid instance location (e.g., IllegalArgumentException wrapped)",
          "description": "If the JsonNodePath cannot be decoded (e.g., contains illegal URI characters), walkAtNode should catch the error and report it as a validation failure rather than throwing."
        }
      ]
    }
  ]
}