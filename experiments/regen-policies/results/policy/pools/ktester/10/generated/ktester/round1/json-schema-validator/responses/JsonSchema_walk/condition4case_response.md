{
  "testCases": [
    {
      "group": "Basic walk – validation enabled",
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
          "expected": "ValidationResult.success() – no validation messages",
          "description": "When validation is turned on and the JSON instance conforms to the schema (a simple object with a required string property), walk should return a successful ValidationResult."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"age\":30})"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure() containing a message about missing required property 'name'",
          "description": "When validation is on but the instance is missing a required property, walk must collect a validation error."
        }
      ]
    },
    {
      "group": "Basic walk – validation disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"age\":30})"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "ValidationResult.success() – no validation messages (validation is skipped)",
          "description": "When the `validate` flag is false, walk must not perform any schema validation, even if the instance is invalid."
        }
      ]
    },
    {
      "group": "Null arguments handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
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
          "expected": "NullPointerException",
          "description": "The method should fail fast when a null ExecutionContext is supplied."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The method should fail fast when a null JsonNode is supplied."
        }
      ]
    },
    {
      "group": "Different JSON node types",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "arrayNode([1,2,3])"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success() – array conforms to schema (type array, items integer)",
          "description": "Walk must correctly handle an array node and validate each element against the schema."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "textNode(\"plain string\")"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure() containing a type‑mismatch message",
          "description": "When the instance is a primitive value that does not match the expected type, walk should report a type‑validation error."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"self\":{}})" 
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success() – recursion resolved without stack overflow",
          "description": "Schema defines a `$recursiveAnchor` and the instance contains a self‑reference; walk must follow the anchor and finish successfully."
        }
      ]
    },
    {
      "group": "Schema resource root flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"id\":\"http://example.com/schema\"})"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success() – schema recognized as resource root, no extra resolution performed",
          "description": "When `isSchemaResourceRoot()` returns true, walk should treat the schema as a top‑level resource and avoid unnecessary ancestor look‑ups."
        }
      ]
    }
  ]
}