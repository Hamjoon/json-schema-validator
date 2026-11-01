{
  "testCases": [
    {
      "group": "walk_validJson_validateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "A well‑formed JSON string is supplied with validation enabled. The walk method should deserialize the JSON, walk the schema and return a successful ValidationResult with no validation messages."
        }
      ]
    },
    {
      "group": "walk_validJson_validateFalse",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "A well‑formed JSON string is supplied but validation is disabled. The walk method should still return a successful ValidationResult (no validation performed)."
        }
      ]
    },
    {
      "group": "walk_invalidJson",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\", \"age\":}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages() contains a parsing error message",
          "description": "Malformed JSON should cause deserialization to fail. The walk method must capture the parsing error and return an invalid ValidationResult with an appropriate message."
        }
      ]
    },
    {
      "group": "walk_emptyString",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages() contains a parsing error indicating empty input",
          "description": "An empty string is not valid JSON. The method should return an invalid ValidationResult with a message about the empty payload."
        }
      ]
    },
    {
      "group": "walk_nullInput",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "Passing a null string should result in a NullPointerException before any deserialization occurs."
        }
      ]
    },
    {
      "group": "walk_validYaml_validateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John\nage: 30"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "A well‑formed YAML document is supplied with validation enabled. The walk method should correctly deserialize YAML, walk the schema and return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "walk_invalidYaml",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John\nage: [unclosed"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages() contains a parsing error for malformed YAML",
          "description": "Malformed YAML input should cause deserialization to fail, resulting in an invalid ValidationResult with a parsing error message."
        }
      ]
    },
    {
      "group": "walk_largeJson",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5},{\"id\":6},{\"id\":7},{\"id\":8},{\"id\":9},{\"id\":10}],\"metadata\":{\"count\":10}}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "A relatively large but still well‑formed JSON payload is processed to ensure the walk method handles bigger inputs without errors."
        }
      ]
    },
    {
      "group": "walk_inputWithSpecialCharacters",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"text\":\"Line1\\nLine2\\tTabbed\\u00A9\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "JSON containing escaped newlines, tabs and Unicode characters should be correctly deserialized and validated."
        }
      ]
    }
  ]
}