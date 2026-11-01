{
  "testCases": [
    {
      "group": "walk_successfulValidation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "Validate a JSON instance that fully conforms to a simple schema (type object with required string 'name' and integer 'age'). The method should return a successful ValidationResult with an empty message set."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": "\"simple string\""
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.success (no validation messages)",
          "description": "Validate a primitive JSON value that matches a schema of type string. Expect a successful ValidationResult."
        }
      ]
    },
    {
      "group": "walk_failureValidation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.failure with messages for type mismatch on 'name' and 'age'",
          "description": "Validate a JSON object where 'name' should be a string but is a number and 'age' should be an integer but is a string. The method must return a ValidationResult containing the appropriate validation messages."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": "[]"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.failure with message indicating array is not allowed by schema",
          "description": "Validate an empty array against a schema that only permits objects. Expect a failure with a clear message."
        }
      ]
    },
    {
      "group": "walk_validateFlagFalse",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": "{\"unexpected\":\"field\"}"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.success (validation skipped)",
          "description": "When the 'validate' flag is false, the method should bypass schema validation and return a successful ValidationResult regardless of the node content."
        }
      ]
    },
    {
      "group": "walk_nullExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": null
            },
            {
              "parameter": "node",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "IllegalArgumentException or NullPointerException",
          "description": "Passing a null ExecutionContext should cause the method to throw an exception because internal processing requires a non‑null context."
        }
      ]
    },
    {
      "group": "walk_nullNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext"
            },
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "IllegalArgumentException or NullPointerException",
          "description": "A null JsonNode is not a valid input; the method should reject it with an appropriate exception."
        }
      ]
    },
    {
      "group": "walk_withCustomExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "customExecutionContextWithFailFast=true"
            },
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":\"invalid\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.failure with a single message (fail‑fast stops after first error)",
          "description": "A custom ExecutionContext configured for fail‑fast should cause the walk method to stop validation after the first encountered error, returning only one validation message."
        }
      ]
    }
  ]
}