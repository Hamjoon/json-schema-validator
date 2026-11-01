{
  "testCases": [
    {
      "group": "Additional properties disallowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"name\":\"John\",\"extra\":\"value\"}"
            }
          ],
          "expected": "non‑empty set containing a message about additional property 'extra' not allowed",
          "description": "The schema defines \"additionalProperties\": false and requires a string property \"name\". Supplying a JSON object that contains an extra property should cause a validation error about disallowed additional properties."
        }
      ]
    },
    {
      "group": "Enum constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"yellow\""
            }
          ],
          "expected": "non‑empty set containing a message that value is not one of the allowed enum values",
          "description": "The schema specifies an enum [\"red\",\"green\",\"blue\"]. Providing a value \"yellow\" which is not listed should trigger an enum‑validation error."
        }
      ]
    },
    {
      "group": "Pattern constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"ab1\""
            }
          ],
          "expected": "non‑empty set containing a message about pattern mismatch",
          "description": "The schema defines a string pattern \"^[A-Z]{3}$\" (exactly three uppercase letters). Supplying \"ab1\" violates the pattern and should be reported."
        }
      ]
    },
    {
      "group": "Number range violations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "5"
            }
          ],
          "expected": "non‑empty set containing a message that the number is less than the minimum (10)",
          "description": "The schema sets \"minimum\": 10 and \"maximum\": 20. Providing a number below the minimum should produce a minimum‑violation message."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "25"
            }
          ],
          "expected": "non‑empty set containing a message that the number exceeds the maximum (20)",
          "description": "The same schema as above; providing a number above the maximum should produce a maximum‑violation message."
        }
      ]
    },
    {
      "group": "Array constraints violations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1]"
            }
          ],
          "expected": "non‑empty set containing a message about array having fewer items than \"minItems\": 2",
          "description": "The schema defines an array with \"minItems\": 2 and \"uniqueItems\": true. Supplying an array with only one element violates the minItems constraint."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[1,1]"
            }
          ],
          "expected": "non‑empty set containing a message about duplicate items when \"uniqueItems\" is true",
          "description": "Using the same array schema, providing duplicate values violates the uniqueItems rule and should be reported."
        }
      ]
    },
    {
      "group": "Nested object required property missing",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"address\":{}}"
            }
          ],
          "expected": "non‑empty set containing a message about missing required property 'city' inside 'address'",
          "description": "The schema defines a property \"address\" of type object with a required string property \"city\". Supplying an empty object for \"address\" should trigger a missing‑property error for \"city\"."
        }
      ]
    },
    {
      "group": "$ref resolution works",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"color\":\"red\"}"
            }
          ],
          "expected": "empty set []",
          "description": "The root schema contains a \"$ref\" to a definition that restricts \"color\" to the enum [\"red\",\"green\"]. Providing a valid enum value should validate successfully, confirming that $ref resolution is performed."
        }
      ]
    },
    {
      "group": "Multiple validation errors aggregated",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{}"
            }
          ],
          "expected": "non‑empty set with at least two messages: missing required property 'name' and missing required property 'age'",
          "description": "The schema requires both \"name\" (string) and \"age\" (integer). An empty JSON object should produce two separate validation messages, demonstrating that the validator aggregates all errors."
        }
      ]
    },
    {
      "group": "Null node accepted when type is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            }
          ],
          "expected": "empty set []",
          "description": "The schema specifies \"type\": \"null\". Supplying a JSON null literal (Jackson's NullNode) should be considered valid and result in no validation messages."
        }
      ]
    },
    {
      "group": "Boolean type validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "true"
            }
          ],
          "expected": "empty set []",
          "description": "The schema defines \"type\": \"boolean\". Providing a proper boolean true should validate without errors."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"true\""
            }
          ],
          "expected": "non‑empty set containing a message about type mismatch (expected boolean)",
          "description": "The same boolean schema; providing a JSON string \"true\" instead of a boolean should trigger a type‑mismatch validation message."
        }
      ]
    }
  ]
}