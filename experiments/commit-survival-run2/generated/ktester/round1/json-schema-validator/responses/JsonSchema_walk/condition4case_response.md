Invalid control character at: line 10 column 47 (char 235)
{
  "testCases": [
    {
      "group": "Normal behavior - validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // default context with no special flags"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"name\":\"John\",\"age\":30}) // valid object matching schema"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage",
          "description": "Standard path where a non‑null ExecutionContext and a valid JSON object are supplied and validation is requested. walk should delegate to walkAtNodeInternal and return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Normal behavior - validation disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"unexpected\":\"field\"}) // any JSON, validation is turned off"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage (validation skipped)",
          "description": "When shouldValidateSchema is false the method should still walk the node but skip any schema validation, resulting in a successful ValidationResult regardless of content."
        }
      ]
    },
    {
      "group": "Edge case - null ExecutionContext",
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
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "walk forwards the ExecutionContext to walkAtNodeInternal; a null context should cause an NPE early in the call chain."
        }
      ]
    },
    {
      "group": "Edge case - null JsonNode",
      "cases": [
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
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "The method passes the same node as both the current and root node to walkAtNodeInternal. Supplying a null node should trigger an NPE before any validation logic runs."
        }
      ]
    },
    {
      "group": "Branch coverage - object with missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectNode({\"age\":30}) // missing required \"name\" property"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains \"required\"",
          "description": "Triggers the branch where the required‑property validator reports a missing field. walk should return an invalid ValidationResult with a corresponding message."
        }
      ]
    },
    {
      "group": "Branch coverage - array node with item validation failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "arrayNode([1, \"string\", 3]) // schema expects all numbers"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains \"type\" for the second element",
          "description": "Ensures the walk logic reaches the branch that validates each array element against the item schema, detecting a type mismatch."
        }
      ]
    },
    {
      "group": "Branch coverage - primitive node (string) with type validator success",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "textNode(\"hello\") // schema type: string"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid() == true and no ValidationMessage",
          "description": "Verifies the path where the TypeValidator confirms the node type matches the schema, resulting in a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Branch coverage - primitive node (number) with type validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "numericNode(42) // schema type: string"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationMessage contains \"type\"",
          "description": "Triggers the branch where the TypeValidator reports a mismatch between the node's actual type (number) and the expected type (string)."
        }
      ]
    }
  ]
}