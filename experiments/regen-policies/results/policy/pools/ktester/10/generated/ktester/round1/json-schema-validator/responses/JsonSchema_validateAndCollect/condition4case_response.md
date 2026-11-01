{
  "testCases": [
    {
      "group": "Null inputs and early exit",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a single ValidationMessage indicating that the instance node is null",
          "description": "When both executionContext and node are null the method should guard against NPE and return an invalid result immediately."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage stating that executionContext is required",
          "description": "If executionContext is null the method should fail early before any validator processing."
        }
      ]
    },
    {
      "group": "Schema without any validators",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (no customizations)"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationResult with isValid() == true and empty message set",
          "description": "When the schema has no required or type validators the method should simply walk the node and return a successful result."
        }
      ]
    },
    {
      "group": "Required validator present – missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"present\":\"value\"}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage containing the missing required property name",
          "description": "The schema declares a required property \"missing\". The node does not contain it, so the required validator should add a message."
        }
      ]
    },
    {
      "group": "Type validator present – type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext"
            },
            {
              "parameter": "node",
              "value": "IntNode representing 123"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage indicating expected type \"string\" but found \"integer\"",
          "description": "The schema expects a string (has a TypeValidator for STRING). Providing an integer should trigger the type‑validation branch."
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
              "value": "mock ExecutionContext with recursiveAnchor flag set to true"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"self\":{}}"
            }
          ],
          "expected": "ValidationResult with isValid() == true (no infinite recursion) and no ValidationMessage about recursion depth",
          "description": "When the schema is marked as a recursive anchor the method must detect and avoid re‑entering the same schema instance, returning a valid result for a self‑referencing node."
        }
      ]
    },
    {
      "group": "WalkListenerRunner present – listener aborts walk",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with WalkListenerRunner that throws WalkAbortException on first node"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"a\":1}"
            }
          ],
          "expected": "ValidationResult with isValid() == false and a ValidationMessage indicating walk was aborted by listener",
          "description": "If a WalkListenerRunner is attached and it aborts the walk, the method should catch the abort and return a result reflecting the early termination."
        }
      ]
    }
  ]
}