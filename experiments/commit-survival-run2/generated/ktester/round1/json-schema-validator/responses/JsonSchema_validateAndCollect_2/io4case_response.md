{
  "testCases": [
    {
      "group": "Enum validator success",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"color\": \"red\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "The schema defines an enum ['red','green','blue'] for property \"color\". The supplied node contains a value that is part of the enum, so the enum validator does not add any messages."
        }
      ]
    },
    {
      "group": "Enum validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"color\": \"yellow\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"enum\" indicating the value is not allowed",
          "description": "The schema defines an enum ['red','green','blue'] for property \"color\". The supplied value \"yellow\" is not in the list, triggering the enum validator."
        }
      ]
    },
    {
      "group": "Const validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"status\": \"pending\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"const\" indicating the constant value mismatch",
          "description": "The schema contains a const keyword with value \"active\" for property \"status\". The node provides a different value, causing the const validator to fail."
        }
      ]
    },
    {
      "group": "Pattern validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"email\": \"not-an-email\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"pattern\" indicating regex mismatch",
          "description": "The schema defines a pattern \"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\" for property \"email\". The supplied string does not match, so the pattern validator adds an error."
        }
      ]
    },
    {
      "group": "Minimum number validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"age\": 15 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"minimum\" indicating the value is less than the allowed minimum",
          "description": "The schema sets a minimum of 18 for property \"age\". The node provides 15, which violates the minimum constraint."
        }
      ]
    },
    {
      "group": "Array items type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"tags\": [ \"valid\", 123, \"alsoValid\" ] }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"type\" for the second array element indicating expected type \"string\" but found \"integer\"",
          "description": "The schema defines \"items\" with a type \"string\" for array property \"tags\". The second element is an integer, causing the items validator to report a type error."
        }
      ]
    },
    {
      "group": "OneOf validator success (one subschema matches)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"value\": 42 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "The schema uses a oneOf with two alternatives: one expects an integer, the other expects a string. The node provides an integer, satisfying the first alternative, so validation succeeds."
        }
      ]
    },
    {
      "group": "OneOf validator failure (none match)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"value\": true }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"oneOf\" indicating that none of the subschemas matched",
          "description": "The schema's oneOf alternatives accept only numbers or strings. The node supplies a boolean, which matches neither, resulting in a oneOf failure."
        }
      ]
    },
    {
      "group": "AllOf validator failure (one subschema fails)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"age\": 25, \"name\": 123 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"type\" for property \"name\" (expected string) while other subschemas may succeed",
          "description": "The schema combines two subschemas via allOf: one requires \"age\" to be >= 18, the other requires \"name\" to be a string. The node satisfies the age constraint but violates the name type, causing allOf to fail."
        }
      ]
    },
    {
      "group": "AnyOf validator success (at least one matches)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"code\": \"A1\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "The schema defines anyOf with two subschemas: one matches a pattern \"^[A-Z][0-9]$\", the other matches a numeric range. The supplied string matches the pattern, so anyOf validation succeeds."
        }
      ]
    },
    {
      "group": "Not validator failure (sub‑schema matches)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"flag\": false }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"not\" indicating the instance matches the prohibited schema",
          "description": "The schema contains a not keyword that disallows the boolean value false for property \"flag\". The node provides false, which matches the inner schema, so the not validator adds an error."
        }
      ]
    },
    {
      "group": "Boolean schema true (always passes)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"any\": \"thing\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "The schema itself is the literal boolean true. According to the spec, any instance validates successfully against a true schema."
        }
      ]
    },
    {
      "group": "Boolean schema false (always fails)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"irrelevant\": 123 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"falseSchema\" indicating that the schema is false",
          "description": "The schema is the literal boolean false. Validation must always fail regardless of the instance."
        }
      ]
    },
    {
      "group": "AdditionalProperties disallowed with extra field",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"known\": \"value\", \"extra\": 42 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"additionalProperties\" indicating that property \"extra\" is not allowed",
          "description": "The schema defines \"additionalProperties\": false and lists only \"known\" as a permitted property. The node contains an unexpected property \"extra\", triggering the additionalProperties validator."
        }
      ]
    },
    {
      "group": "Discriminator handling – matching mapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"cat\", \"meow\": true }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "The schema uses a discriminator on property \"type\" with a mapping { \"cat\": \"#/components/schemas/Cat\" }. The node's discriminator value \"cat\" resolves to the Cat schema, which validates successfully."
        }
      ]
    },
    {
      "group": "Discriminator handling – unmapped value",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"type\": \"dragon\", \"fire\": true }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"discriminator\" indicating that the discriminator value \"dragon\" has no mapping",
          "description": "The discriminator mapping does not contain an entry for \"dragon\". The method must record a discriminator‑error message."
        }
      ]
    },
    {
      "group": "External $ref resolution failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"http://example.com/nonexistent.json\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"$ref\" indicating that the external reference could not be retrieved",
          "description": "The schema contains a $ref pointing to an external URI that cannot be fetched. The validator should add an appropriate $ref resolution error."
        }
      ]
    },
    {
      "group": "Circular $ref detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"#/components/schemas/Node\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"circularReference\" indicating a reference loop was detected",
          "description": "Schema A references Schema B via $ref, and Schema B references back to A, forming a cycle. The validator must detect the loop and report a circular reference error."
        }
      ]
    }
  ]
}