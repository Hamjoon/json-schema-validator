{
  "testCases": [
    {
      "group": "Null checks and argument validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"name\":\"John\"})"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'executionContext must not be null'",
          "description": "The method should reject a null ExecutionContext and throw an IllegalArgumentException before any validation work is performed."
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
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'node must not be null'",
          "description": "A null JsonNode is not a valid input; the method must validate arguments and throw an appropriate exception."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "valid JsonNode (e.g., {\"age\":30})"
            },
            {
              "parameter": "format",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'format must not be null'",
          "description": "The OutputFormat is mandatory; passing null should result in an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Successful validation (no errors)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}} validated against instance {\"name\":\"Alice\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (collects ValidationMessage objects into a List)"
            }
          ],
          "expected": "Empty List (no ValidationMessage objects)",
          "description": "When the instance conforms to the schema, the validator should produce no validation messages; the OutputFormat should return an empty collection."
        }
      ]
    },
    {
      "group": "Failed validation (error messages produced)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}} validated against instance {\"age\":\"twenty\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List with at least one ValidationMessage indicating type mismatch for property 'age'",
          "description": "The instance violates the schema (string instead of integer). The validator must detect the error and the OutputFormat must return a list containing the corresponding ValidationMessage."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}} validated against instance {}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage about missing required property 'id'",
          "description": "Missing required property should be reported as a validation error."
        }
      ]
    },
    {
      "group": "Custom OutputFormat behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a valid schema and instance (e.g., simple string schema \"type\":\"string\" with instance \"hello\")"
            },
            {
              "parameter": "format",
              "value": "BooleanOutputFormat (returns true if no errors, false otherwise)"
            }
          ],
          "expected": "true",
          "description": "A custom OutputFormat that converts the validation result to a boolean should return true when validation succeeds."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a schema requiring an integer but instance is a boolean"
            },
            {
              "parameter": "format",
              "value": "BooleanOutputFormat"
            }
          ],
          "expected": "false",
          "description": "The same BooleanOutputFormat should return false when the validation produces at least one error."
        }
      ]
    },
    {
      "group": "Recursive anchor handling (schema with $recursiveAnchor)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a schema that contains a $recursiveAnchor and validates a self‑referencing structure (e.g., a linked‑list node schema)"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "Empty List (validation succeeds without infinite recursion)",
          "description": "When the schema is marked as a recursive anchor, the validator must correctly handle self‑references and terminate; no validation messages should be produced for a conforming instance."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing the same recursive schema but the instance violates a nested constraint (e.g., a node's \"value\" field is of wrong type)"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage for the nested violation",
          "description": "Even with recursion, the validator must still report errors that occur deep inside the structure."
        }
      ]
    }
  ]
}