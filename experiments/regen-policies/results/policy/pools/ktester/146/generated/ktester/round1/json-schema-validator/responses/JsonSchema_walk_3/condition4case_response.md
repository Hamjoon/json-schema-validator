Invalid control character at: line 10 column 47 (char 254)
{
  "testCases": [
    {
      "group": "Walk with valid ExecutionContext and validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // default, no customizer"
            },
            {
              "parameter": "node",
              "value": "objectMapper.readTree(\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isSuccess() == true (no validation errors)",
          "description": "A normal walk where a non‑null ExecutionContext is supplied, the JSON node conforms to the schema and validation is turned on. The method should return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Walk with valid ExecutionContext and validation disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectMapper.readTree(\"{\\\"unexpected\\\":\\\"field\\\"}\")"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isSuccess() == true (validation is skipped)",
          "description": "When the validate flag is false the walk should bypass all validators and always report success, even if the instance does not match the schema."
        }
      ]
    },
    {
      "group": "Walk with null ExecutionContext (fallback to default)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "objectMapper.readTree(\"{\\\"id\\\":\\\"123\\\"}\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isSuccess() == true (method creates a default ExecutionContext internally)",
          "description": "The implementation should detect a null ExecutionContext and create a fresh one internally. Validation should still be performed and succeed for a matching node."
        }
      ]
    },
    {
      "group": "Walk with null JsonNode (error path)",
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
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "Passing a null node should cause the method to fail early (the internal walkAtNodeInternal expects a non‑null node). The test verifies that a NullPointerException is thrown."
        }
      ]
    },
    {
      "group": "Walk where schema validation fails (error collection)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectMapper.readTree(\"{\\\"age\\\":\\\"not-a-number\\\"}\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and ValidationResult.getMessages().size() > 0",
          "description": "The node violates the schema (e.g., age should be an integer). With validation enabled the walk must collect validation messages and return an unsuccessful ValidationResult."
        }
      ]
    },
    {
      "group": "Walk with $ref resolution (recursive anchor handling)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "objectMapper.readTree(\"{\\\"$ref\\\":\\\"#/definitions/person\\\"}\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isSuccess() == true if the referenced definition is valid, otherwise false",
          "description": "Ensures that walk correctly follows $ref nodes and respects the isRecursiveAnchor flag. The test uses a schema that contains a definition and verifies that the walk either succeeds or fails according to the resolved sub‑schema."
        }
      ]
    }
  ]
}