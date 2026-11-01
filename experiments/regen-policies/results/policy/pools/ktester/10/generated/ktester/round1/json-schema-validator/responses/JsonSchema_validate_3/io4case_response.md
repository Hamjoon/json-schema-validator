{
  "testCases": [
    {
      "group": "Array Schema Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing [1, 2, 3]"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty set",
          "description": "Schema defines type: \"array\" with items of type \"integer\". A valid integer array should produce no validation messages."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing [1, \"two\", 3]"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "set containing message: ['type mismatch for array element at index 1']",
          "description": "Array schema with integer items receives a non‑integer element; validator must report the type mismatch for the offending index."
        }
      ]
    },
    {
      "group": "Boolean Schema Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"any\":\"value\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty set",
          "description": "Schema is the boolean literal true (accepts any JSON). Validation should always succeed regardless of the document."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"any\":\"value\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "set containing message: ['instance does not match schema (boolean false)']",
          "description": "Schema is the boolean literal false (rejects any JSON). Validation must always return a failure message."
        }
      ]
    },
    {
      "group": "Complex Constraint Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"id\":\"123e4567-e89b-12d3-a456-426614174000\",\"age\":25,\"email\":\"john@example.com\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty set",
          "description": "Schema combines multiple constraints: \"id\" must match UUID pattern, \"age\" integer minimum 18, \"email\" format \"email\". All constraints satisfied → no messages."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"id\":\"not-a-uuid\",\"age\":15,\"email\":\"not-an-email\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "set containing messages: ['pattern mismatch for property id', 'value 15 is less than minimum 18 for property age', 'format validation failed for property email']",
          "description": "Same schema as above but each property violates a different constraint, exercising pattern, minimum, and format validation branches."
        }
      ]
    },
    {
      "group": "Reference ($ref) Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"address\":{\"street\":\"Main St\",\"zip\":\"12345\"}}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty set",
          "description": "Schema contains a $ref to an external definition for \"address\" (type object with required street and zip). The instance complies, confirming that `fromRef` resolution works."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"address\":{\"street\":10,\"zip\":\"12345\"}}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "set containing message: ['type mismatch for property street in $ref address']",
          "description": "Same $ref schema but the referenced object has a type error, ensuring the validator reports errors originating from referenced schemas."
        }
      ]
    },
    {
      "group": "Custom ExecutionContext Flags Interaction",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that sets ExecutionContext.setValidateSchema(false)"
            }
          ],
          "expected": "empty set",
          "description": "Disabling schema validation via the execution context should suppress all validation messages, even if required properties are missing."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that sets ExecutionContext.setValidateSchema(true) and ExecutionContext.setValidateSchemaRoot(false)"
            }
          ],
          "expected": "set containing message: ['required property age is missing']",
          "description": "Enabling validation but marking the current schema as not a root should still validate the instance, confirming that `isSchemaResourceRoot` flag is respected."
        }
      ]
    },
    {
      "group": "Invalid ExecutionCustomizer Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that throws IllegalArgumentException(\"bad customizer\")"
            }
          ],
          "expected": "IllegalArgumentException with message 'bad customizer'",
          "description": "When the consumer throws a checked exception wrapped in a runtime exception, `validate` must propagate the original exception."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that modifies ExecutionContext and then throws NullPointerException"
            }
          ],
          "expected": "NullPointerException",
          "description": "Ensures that any exception (including NPE) thrown after mutating the context is not swallowed by the validator."
        }
      ]
    }
  ]
}