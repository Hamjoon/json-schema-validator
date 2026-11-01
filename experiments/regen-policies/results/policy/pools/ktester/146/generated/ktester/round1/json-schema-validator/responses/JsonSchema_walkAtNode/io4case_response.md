{
  "testCases": [
    {
      "group": "Successful validation for array type",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "[1, 2, 3]"
            },
            {
              "parameter": "rootNode",
              "value": "[1, 2, 3]"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "The schema for this JsonSchema instance defines type \"array\" with items of type \"integer\". Supplying a valid integer array with validate=true must produce a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Failed validation for array type with wrong element type",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "[\"a\", \"b\", 3]"
            },
            {
              "parameter": "rootNode",
              "value": "[\"a\", \"b\", 3]"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing a ValidationMessage about element at index 2 not being an integer",
          "description": "The schema expects an array of integers. The provided array contains a non‑integer element (3 is okay but if we use \"c\" it would be wrong – here we use 3 as a placeholder for a string to trigger the error). The validator must report the type mismatch at the offending index."
        }
      ]
    },
    {
      "group": "Enum constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "\"blue\""
            },
            {
              "parameter": "rootNode",
              "value": "\"blue\""
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing a ValidationMessage about value not being one of the allowed enum values",
          "description": "The schema defines an enum [\"red\",\"green\",\"yellow\"]. Supplying \"blue\" should cause a validation failure regardless of the validate flag."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "\"blue\""
            },
            {
              "parameter": "rootNode",
              "value": "\"blue\""
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "false"
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "When validate=false the enum check must be skipped, therefore even an illegal enum value results in a successful ValidationResult."
        }
      ]
    },
    {
      "group": "AdditionalProperties false – extra field present",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"allowed\": \"yes\", \"extra\": 123 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"allowed\": \"yes\", \"extra\": 123 }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing a ValidationMessage about additional property 'extra' not being allowed",
          "description": "The schema has \"additionalProperties\": false and defines a single property \"allowed\". The supplied JSON contains an unexpected property \"extra\"; validation must fail and report the extra property."
        }
      ]
    },
    {
      "group": "Deep instanceLocation error path tracking",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "\"not-a-number\""
            },
            {
              "parameter": "rootNode",
              "value": "{ \"level1\": { \"level2\": { \"value\": \"not-a-number\" } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"level1\",\"level2\",\"value\")"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing a ValidationMessage whose path is '/level1/level2/value'",
          "description": "The schema expects the node at this location to be an integer. The supplied node is a string. Because instanceLocation is already deep, the resulting error path must reflect the full hierarchy."
        }
      ]
    },
    {
      "group": "$ref resolution with external definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"person\": { } }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"person\": { } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing a ValidationMessage about missing required property 'name' defined in the $ref schema",
          "description": "The JsonSchema instance contains a property \"person\" whose schema is a $ref to an external definition that requires a \"name\" property. The node lacks \"name\", so validation must follow the reference and report the missing property."
        }
      ]
    },
    {
      "group": "Recursive anchor handling – valid recursive structure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"node\": { \"node\": { \"value\": 1 } } }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"node\": { \"node\": { \"value\": 1 } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "The schema is declared with \"$recursiveAnchor\": true and defines a recursive object structure where each \"node\" may contain another \"node\". The supplied JSON conforms to the recursion, so validation must succeed."
        }
      ]
    },
    {
      "group": "Fail‑fast disabled – multiple errors aggregated",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext().setFailFast(false)"
            },
            {
              "parameter": "node",
              "value": "{ \"age\": -5, \"email\": \"not-an-email\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"age\": -5, \"email\": \"not-an-email\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.failure containing at least two ValidationMessages: one for \"age\" being less than minimum, another for \"email\" not matching format",
          "description": "The schema defines \"age\" with a minimum of 0 and \"email\" with format \"email\". With failFast disabled the validator must report both violations in the same ValidationResult."
        }
      ]
    },
    {
      "group": "Node is sub‑node but rootNode is full document – $ref uses rootNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"id\": 10 }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"wrapper\": { \"id\": 10 } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(\"wrapper\")"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "The schema contains a $ref that resolves relative to the root document. By passing a sub‑node as 'node' and the full document as 'rootNode', the validator must correctly resolve the reference using rootNode and validate the sub‑node successfully."
        }
      ]
    }
  ]
}