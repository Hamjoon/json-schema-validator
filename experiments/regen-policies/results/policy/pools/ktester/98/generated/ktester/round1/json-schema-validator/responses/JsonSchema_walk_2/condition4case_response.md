{
  "testCases": [
    {
      "group": "Null node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains a message indicating that the instance node is null",
          "description": "When the supplied JsonNode is null the method should immediately return an invalid ValidationResult with an appropriate error message, regardless of the validate flag."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains a message indicating that the instance node is null",
          "description": "Even when validation is disabled, a null instance node is considered an error and must be reported."
        }
      ]
    },
    {
      "group": "Validate flag true – valid instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and getMessages().isEmpty()",
          "description": "A well‑formed JSON node that satisfies the schema should produce a successful ValidationResult when validation is enabled."
        }
      ]
    },
    {
      "group": "Validate flag true – invalid instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains messages for type mismatches on 'name' and 'age'",
          "description": "When validation is enabled and the instance violates type constraints, the walk method must return an invalid ValidationResult with the corresponding error messages."
        }
      ]
    },
    {
      "group": "Validate flag false – skip validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isValid() == true and getMessages().isEmpty()",
          "description": "When validation is disabled the method should walk the schema tree without performing any checks, therefore it must return a successful ValidationResult even if the instance is technically invalid."
        }
      ]
    },
    {
      "group": "Recursive anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing a self‑referencing structure that triggers a recursive anchor"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and no StackOverflowError is thrown",
          "description": "If the schema contains a recursive anchor, walk must detect it via JsonSchema.isRecursiveAnchor() and avoid infinite recursion, returning a valid result for a correctly‑structured instance."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing a self‑referencing structure that violates the recursive anchor constraints"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains a message describing the recursive anchor violation",
          "description": "When the recursive anchor is present but the instance does not satisfy the anchor’s schema, walk must report the violation rather than looping forever."
        }
      ]
    },
    {
      "group": "Schema resource root handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing a valid instance for a schema that is marked as a resource root (JsonSchema.isSchemaResourceRoot() == true)"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and getMessages().isEmpty()",
          "description": "When the current schema is the resource root, walk should still perform normal validation and return a successful result for a valid instance."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing an invalid instance for a schema that is marked as a resource root"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains appropriate validation messages",
          "description": "Even for a resource‑root schema, validation errors must be reported correctly."
        }
      ]
    },
    {
      "group": "Exception handling – unsupported encoding in $ref resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing a $ref with an illegal URL‑encoded fragment"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and contains a message about UnsupportedEncodingException",
          "description": "If a $ref contains a fragment that cannot be URL‑decoded, walk should catch UnsupportedEncodingException and return an invalid ValidationResult with a clear error message."
        }
      ]
    }
  ]
}