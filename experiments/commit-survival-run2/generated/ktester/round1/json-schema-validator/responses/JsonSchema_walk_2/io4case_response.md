{
  "testCases": [
    {
      "group": "Validate schema flag true - array items valid",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema defines a property of type array where each item must be an object with required integer \"id\" and string \"name\". The supplied array conforms to the item schema, so walk should succeed."
        }
      ]
    },
    {
      "group": "Validate schema flag true - array items invalid",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[{\"id\":\"one\",\"name\":\"Alice\"}, {\"id\":2}]"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing type‑mismatch for \"id\" and missing‑required‑property for \"name\" in the second array element (isSuccess() == false)",
          "description": "Same array schema as above, but the first element has a non‑integer \"id\" and the second element lacks the required \"name\" property. walk must report both errors."
        }
      ]
    },
    {
      "group": "Validate schema flag true - enum validation success",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"RED\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema defines a string with an enum [\"RED\",\"GREEN\",\"BLUE\"]. The node value matches one of the allowed enum values, so validation succeeds."
        }
      ]
    },
    {
      "group": "Validate schema flag true - enum validation failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"YELLOW\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing enum‑value ValidationMessage (isSuccess() == false)",
          "description": "The same enum schema as above, but the node value \"YELLOW\" is not listed in the enum, causing a validation error."
        }
      ]
    },
    {
      "group": "Validate schema flag true - pattern property match",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"email\":\"john.doe@example.com\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema uses \"patternProperties\" with the regex \"^email$\" requiring the value to match the email pattern. The supplied email matches, so validation passes."
        }
      ]
    },
    {
      "group": "Validate schema flag true - pattern property mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"email\":\"not-an-email\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing format‑validation ValidationMessage for property \"email\" (isSuccess() == false)",
          "description": "Same patternProperties schema, but the value does not satisfy the email format, resulting in a validation error."
        }
      ]
    },
    {
      "group": "Validate schema flag true - format validation success (date-time)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"2023-08-15T13:45:30Z\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema defines a string with format \"date-time\". The supplied ISO‑8601 timestamp is valid, so walk should succeed."
        }
      ]
    },
    {
      "group": "Validate schema flag true - format validation failure (date-time)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"15/08/2023 13:45\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing format‑validation ValidationMessage (isSuccess() == false)",
          "description": "Same date-time format schema, but the string does not conform to the required RFC3339 format, causing a validation error."
        }
      ]
    },
    {
      "group": "Validate schema flag true - $ref resolves correctly",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"type\":\"car\",\"wheels\":4}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema contains a $ref to \"#/definitions/vehicle\" where the definition requires a string \"type\" and integer \"wheels\". The node matches the referenced definition, so walk resolves the $ref and validates successfully."
        }
      ]
    },
    {
      "group": "Validate schema flag true - boolean schema true (always valid)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "The schema is the literal boolean true. According to JSON‑Schema spec, any instance is valid against a true schema, so walk must return success regardless of the node content."
        }
      ]
    },
    {
      "group": "Validate schema flag true - boolean schema false (always invalid)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"anything\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating instance does not match boolean false schema (isSuccess() == false)",
          "description": "The schema is the literal boolean false. No instance can satisfy it, so walk must always return a failure."
        }
      ]
    },
    {
      "group": "Validate schema flag true - additionalProperties false with extra property",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"id\":1,\"extra\":\"not allowed\"}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing additionalProperties ValidationMessage for \"extra\" (isSuccess() == false)",
          "description": "Schema defines an object with property \"id\" and sets \"additionalProperties\": false. The supplied node contains an unexpected property \"extra\", which must be reported as an error."
        }
      ]
    },
    {
      "group": "Validate schema flag true - anyOf with one matching subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "42"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema uses \"anyOf\" with two subschemas: one requiring a string, the other requiring an integer. The node is an integer, satisfying the second subschema, so overall validation succeeds."
        }
      ]
    },
    {
      "group": "Validate schema flag true - anyOf with none matching",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "true"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing anyOf‑validation ValidationMessage (isSuccess() == false)",
          "description": "Same anyOf schema as above, but the node is a boolean, which matches neither subschema, leading to a validation failure."
        }
      ]
    },
    {
      "group": "Validate schema flag true - recursive schema (self‑reference) valid",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"value\":1,\"next\":{\"value\":2,\"next\":null}}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult with no errors (isSuccess() == true)",
          "description": "Schema defines a recursive object where \"next\" is either null or another object of the same schema. The supplied linked‑list structure conforms, so walk should succeed without infinite recursion."
        }
      ]
    },
    {
      "group": "Validate schema flag true - recursive schema (self‑reference) invalid",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"value\":\"not an int\",\"next\":{}}"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "ValidationResult containing type‑mismatch for \"value\" and possibly missing‑required‑property for \"next.value\" (isSuccess() == false)",
          "description": "Same recursive schema, but the root \"value\" is a string instead of the required integer and the nested \"next\" object lacks required fields. walk must detect both errors."
        }
      ]
    }
  ]
}