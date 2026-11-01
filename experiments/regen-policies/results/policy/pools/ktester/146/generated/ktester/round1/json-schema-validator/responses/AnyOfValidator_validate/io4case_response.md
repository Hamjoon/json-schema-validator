{
  "newTestCases": [
    {
      "group": "anyOf validation succeeds with short‑circuit on after first failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit enabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"integer\", \"value\": 42 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"integer\", \"value\": 42 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – first subschema (type: string) fails, second subschema (type: integer, minimum: 0) succeeds, validator short‑circuits after the successful second subschema, so remaining subschemas are not evaluated.",
          "description": "When short‑circuiting is enabled the validator must continue iterating after a failure until a subschema validates, then stop immediately. This test confirms that only the necessary subschemas are evaluated."
        }
      ]
    },
    {
      "group": "anyOf with empty array (always fails)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (any configuration)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"anything\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"string\", \"value\": \"anything\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "Set<ValidationMessage> containing a single message indicating that anyOf failed because the anyOf array is empty.",
          "description": "According to the JSON‑Schema specification an anyOf keyword with an empty array cannot be satisfied, therefore validation must always produce an error regardless of the instance."
        }
      ]
    },
    {
      "group": "anyOf with a single subschema – passing instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit disabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"number\", \"value\": 3.14 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"number\", \"value\": 3.14 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – the single subschema (type: number, multipleOf: 0.01) validates successfully, so anyOf succeeds.",
          "description": "A single‑element anyOf should behave exactly like that element. This test verifies the happy path when the instance satisfies the sole subschema."
        }
      ]
    },
    {
      "group": "anyOf with a single subschema – failing instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit disabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"number\", \"value\": -5 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"number\", \"value\": -5 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "Set<ValidationMessage> containing the validation error(s) produced by the single subschema (e.g., minimum: 0 violation).",
          "description": "When anyOf contains only one subschema, a failing instance must return the errors of that subschema. This test ensures proper error propagation."
        }
      ]
    },
    {
      "group": "anyOf validation with array instance and mixed subschemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit enabled)"
            },
            {
              "parameter": "node",
              "value": "[ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "rootNode",
              "value": "[ \"a\", \"b\", \"c\" ]"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – the anyOf array contains two subschemas: (1) type: array, minItems: 2 and (2) type: string, enum: [\"x\",\"y\"]. The instance matches the first subschema, causing short‑circuit success.",
          "description": "Validates that AnyOf correctly evaluates array‑type subschemas and short‑circuits after the first matching array subschema, ignoring later non‑array alternatives."
        }
      ]
    },
    {
      "group": "anyOf validation where rootNode differs from node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit disabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"string\", \"value\": \"inner\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"outer\": { \"type\": \"string\", \"value\": \"inner\" } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$.outer\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – the validator uses the supplied node for validation but keeps the original rootNode for $ref resolution; since the node satisfies one of the anyOf alternatives, validation succeeds.",
          "description": "Ensures that the validator correctly handles the case where the instance node is a child of the root document, preserving proper $ref and $id handling."
        }
      ]
    },
    {
      "group": "walk flag true with short‑circuit enabled (deep validation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext (short‑circuit enabled)"
            },
            {
              "parameter": "node",
              "value": "{ \"type\": \"object\", \"inner\": { \"type\": \"integer\", \"value\": 7 } }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"inner\": { \"type\": \"integer\", \"value\": 7 } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "new JsonNodePath(\"$\")"
            }
          ],
          "expected": "empty Set<ValidationMessage> – the protected validate method is called with walk=true; the validator traverses into the nested integer node, finds a matching subschema, short‑circuits, and returns no errors.",
          "description": "Combines deep traversal (walk flag) with short‑circuit behavior to verify that the validator stops further recursion once a matching subschema is found."
        }
      ]
    }
  ]
}