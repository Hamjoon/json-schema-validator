Invalid control character at: line 10 column 47 (char 228)
{
  "testCases": [
    {
      "group": "validate true – schema passes",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // default context"
            },
            {
              "parameter": "node",
              "value": "ObjectMapper().readTree(\"{\\\"age\\\":30}\\\") // matches schema {\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "When validate flag is true and the JSON node conforms to the schema, walk should return a successful ValidationResult with no messages."
        }
      ]
    },
    {
      "group": "validate true – schema fails",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "ObjectMapper().readTree(\"{\\\"age\\\":\\\"thirty\\\"}\\\") // type mismatch (string instead of integer)"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages().size() > 0 (contains a type‑error message)",
          "description": "When validate flag is true but the JSON node does not satisfy the schema, walk should return an invalid ValidationResult containing at least one ValidationMessage."
        }
      ]
    },
    {
      "group": "validate false – validation skipped",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "ObjectMapper().readTree(\"{\\\"age\\\":\\\"any\\\"}\\\") // deliberately invalid"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "When validate flag is false the walk method must skip schema validation entirely, returning a successful ValidationResult regardless of node content."
        }
      ]
    },
    {
      "group": "null ExecutionContext – default handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "ObjectMapper().readTree(\"{\\\"age\\\":30}\\\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no exception thrown",
          "description": "If a null ExecutionContext is supplied, walk should internally create a default ExecutionContext (as per library contract) and still perform validation correctly."
        }
      ]
    },
    {
      "group": "walk overload delegating to focal method",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectMapper().readTree(\"{\\\"age\\\":30}\\\")"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "Result identical to calling walk(createExecutionContext(), node, true)",
          "description": "The overload walk(JsonNode, boolean) must delegate to the focal method; this test verifies that the returned ValidationResult matches the direct call."
        }
      ]
    }
  ]
}