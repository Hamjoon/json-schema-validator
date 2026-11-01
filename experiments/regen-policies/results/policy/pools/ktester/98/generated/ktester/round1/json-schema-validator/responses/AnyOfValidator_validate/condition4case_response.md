{
  "testCases": [
    {
      "group": "AnyOfValidator - node matches at least one subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"hello\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"hello\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set<ValidationMessage> (no errors)",
          "description": "The AnyOfValidator should succeed when the instance node satisfies at least one of the subschemas defined in the \"anyOf\" keyword. The test uses a simple string schema as one of the alternatives, so validation must return an empty set."
        }
      ]
    },
    {
      "group": "AnyOfValidator - node matches multiple subschemas (short‑circuit true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextShortCircuitEnabled"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"number\",\"value\":5})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"number\",\"value\":5})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When more than one subschema validates successfully, the validator may short‑circuit after the first successful match. This case verifies that no ValidationMessage is produced and that the internal short‑circuit flag is honoured."
        }
      ]
    },
    {
      "group": "AnyOfValidator - node matches none of the subschemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"boolean\",\"value\":true})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"boolean\",\"value\":true})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set<ValidationMessage> containing a single message with type='anyOf' and message='instance does not match any schema in anyOf'",
          "description": "The validator must return a ValidationMessage when the instance fails to satisfy any of the schemas listed under \"anyOf\"."
        }
      ]
    },
    {
      "group": "AnyOfValidator - empty anyOf array",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextEmptyAnyOf"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"null\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"null\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set<ValidationMessage> containing a message indicating that anyOf must contain at least one schema",
          "description": "If the schema's anyOf array is empty, validation should fail immediately with a clear error message."
        }
      ]
    },
    {
      "group": "AnyOfValidator - null node (instance is missing)",
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
              "value": "jsonNode({\"type\":\"object\",\"properties\":{}})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "Set<ValidationMessage> containing a message that the instance is missing and cannot be validated against anyOf",
          "description": "When the instance node is null, the validator should treat it as a validation failure unless the anyOf subschemas explicitly allow null."
        }
      ]
    },
    {
      "group": "AnyOfValidator - walk flag set to true (internal validation of subschemas)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWalkTrue"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"object\",\"properties\":{\"a\":1}})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"object\",\"properties\":{\"a\":1}})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set<ValidationMessage> (walk succeeded)",
          "description": "The protected overload validate(..., boolean walk) is exercised via a subclass that exposes it. When walk is true the validator must recursively walk each subschema; this test ensures that a matching subschema still results in no errors."
        }
      ]
    },
    {
      "group": "AnyOfValidator - canShortCircuit() returns false (full evaluation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextNoShortCircuit"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"test\"})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"string\",\"value\":\"test\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "When canShortCircuit() is overridden to return false, the validator must evaluate all subschemas even after a successful match. The test verifies that no ValidationMessage is produced and that the method does not exit early."
        }
      ]
    },
    {
      "group": "AnyOfValidator - preloadJsonSchema invoked before validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNode({\"type\":\"integer\",\"value\":10})"
            },
            {
              "parameter": "rootNode",
              "value": "jsonNode({\"type\":\"integer\",\"value\":10})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            }
          ],
          "expected": "empty Set<ValidationMessage> and preloadJsonSchema called exactly once",
          "description": "The validator should call preloadJsonSchema() the first time validation is performed to resolve any referenced schemas. The test asserts that the call occurs and that validation still succeeds."
        }
      ]
    }
  ]
}