{
  "tests": [
    {
      "group": "Valid input with required property missing – validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains a 'required' error for property 'name'",
          "description": "The JSON document does not contain the required property 'name'. Walk should parse the document, run validation and return a failure result with the appropriate required‑property message."
        }
      ]
    },
    {
      "group": "Valid JSON array input against object schema – validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "[{\"type\":\"object\"}]"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage mentions type mismatch (expected object, found array)",
          "description": "The schema expects an object at the root, but the input is a JSON array. Walk must detect the type mismatch and report it."
        }
      ]
    },
    {
      "group": "Valid YAML input – validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  id:\n    type: integer"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors",
          "description": "When the library supports YAML, a well‑formed YAML schema should be deserialized, validated and return a successful result."
        }
      ]
    },
    {
      "group": "Recursive schema anchor – validation succeeds without stack overflow",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"$id\":\"#\",\"$recursiveAnchor\":true,\"type\":\"object\",\"properties\":{\"self\":{\"$ref\":\"#\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors (walk terminates correctly)",
          "description": "The schema contains a recursive anchor referencing itself. Walk must handle the recursion guard and finish without infinite recursion."
        }
      ]
    },
    {
      "group": "Schema with $ref to external definition – reference resolved",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"address\":{\"$ref\":\"#/definitions/address\"}},\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors",
          "description": "The schema uses a $ref to an internal definition. Walk must resolve the reference and validate successfully."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer disables format validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\",\"format\":\"email\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.getValidationContext().getConfig().setFormatEnabled(false)"
            }
          ],
          "expected": "ValidationResult.success == true and no ValidationMessage errors (format validator disabled)",
          "description": "The customizer turns off format validation. Even though the instance contains an invalid email format, walk should succeed because the format check is disabled."
        }
      ]
    },
    {
      "group": "Validate flag false but customizer throws exception – exception propagates",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message 'customizer failure' is thrown",
          "description": "Even when validation is disabled, the supplied ExecutionContextCustomizer must be invoked. An exception thrown by the customizer should propagate out of walk."
        }
      ]
    },
    {
      "group": "Empty string input – deserialization failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "ValidationResult.success == false and ValidationMessage contains a parsing error indicating empty input",
          "description": "An empty string is not valid JSON. Walk should catch the deserialization error and return a failed ValidationResult."
        }
      ]
    }
  ]
}