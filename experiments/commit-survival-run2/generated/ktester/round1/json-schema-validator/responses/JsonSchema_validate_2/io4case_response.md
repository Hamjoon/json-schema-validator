{
  "groups": [
    {
      "group": "Pattern validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"code\": \"AB1234\" }"
            }
          ],
          "expected": "Empty Set – the string matches the regex pattern ^[A-Z]{2}\\d{4}$",
          "description": "Schema defines property \"code\" with a pattern that requires two uppercase letters followed by four digits. The instance value conforms, exercising the successful pattern‑validator path."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"code\": \"ab12\" }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"pattern mismatch: value 'ab12' does not match '^\\\\[A-Z]{2}\\\\d{4}$'\"",
          "description": "The same pattern constraint is applied, but the instance value does not satisfy the regex, triggering the pattern‑validator branch."
        }
      ]
    },
    {
      "group": "Array items and size validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[ 1, 2, 3 ]"
            }
          ],
          "expected": "Empty Set – all items are integers and array length respects minItems=1 and maxItems=5",
          "description": "Schema defines an array of integers with minItems=1, maxItems=5, and each item must be of type integer. The instance meets both size and item‑type constraints."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[ 1, \"two\", 3, 4, 5, 6 ]"
            }
          ],
          "expected": "Set containing two ValidationMessages: (1) type mismatch for item at index 1 (expected integer, found string), (2) array size exceeds maxItems of 5",
          "description": "The array contains a string among integers (type‑validator failure) and has six elements, violating the maxItems constraint, exercising multiple validator branches."
        }
      ]
    },
    {
      "group": "Enum and const validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"RED\""
            }
          ],
          "expected": "Empty Set – value is one of the allowed enum values [\"RED\",\"GREEN\",\"BLUE\"]",
          "description": "Schema defines an enum of colour strings. The instance value \"RED\" is permitted, testing the enum‑validator happy path."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"YELLOW\""
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"enum mismatch: value 'YELLOW' is not among the allowed values\"",
          "description": "The same enum constraint is applied, but the instance value is not listed, causing an enum‑validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "42"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"const mismatch: value 42 does not equal the required constant 100\"",
          "description": "Schema defines a const value of 100. The instance provides 42, exercising the const‑validator failure branch."
        }
      ]
    },
    {
      "group": "Composite keywords (allOf / anyOf / oneOf)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": 30, \"email\": \"user@example.com\" }"
            }
          ],
          "expected": "Empty Set – instance satisfies all subschemas combined via allOf",
          "description": "Schema uses allOf to require both a numeric \"age\" (minimum 0) and a string \"email\" matching an email format. The instance meets both, testing the allOf aggregation."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"short\""
            }
          ],
          "expected": "Empty Set – instance satisfies at least one subschema in anyOf (e.g., string with minLength 5 or integer > 10) because it matches the integer subschema after coercion is not applied, but matches the string subschema with minLength 5? Actually \"short\" length 5 satisfies minLength 5, so passes.",
          "description": "Schema defines anyOf with two alternatives: a string with minLength 5, or an integer greater than 10. The instance is a string of length 5, satisfying one alternative, exercising the anyOf success path."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "15"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"oneOf validation failed: instance matches 2 subschemas, expected exactly 1\"",
          "description": "Schema defines oneOf with two mutually exclusive subschemas (integer > 10 and integer < 20). The value 15 satisfies both, violating the oneOf rule and triggering the corresponding validation error."
        }
      ]
    },
    {
      "group": "Conditional validation (if/then/else)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"premium\", \"discount\": 20 }"
            }
          ],
          "expected": "Empty Set – condition matches (type == \"premium\") and then‑schema validates discount property",
          "description": "Schema uses if to check \"type\" equals \"premium\"; then requires a numeric \"discount\" between 0 and 50. The instance satisfies both the condition and the then‑branch."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"premium\", \"discount\": 60 }"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"then validation failed: 'discount' must be <= 50\"",
          "description": "Condition is true, but the then‑schema's maximum constraint on \"discount\" is violated, exercising the then‑validation failure path."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"basic\", \"extra\": \"value\" }"
            }
          ],
          "expected": "Empty Set – condition is false, else‑schema (which allows additional properties) validates successfully",
          "description": "The if condition does not match, so the else‑schema is applied; it permits the \"extra\" property, resulting in no validation messages."
        }
      ]
    },
    {
      "group": "Root node type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"just a string\""
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"type mismatch: expected object but found string\"",
          "description": "Schema expects the root instance to be an object (e.g., defines required properties). Providing a primitive string triggers the top‑level type‑validator."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "12345"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"type mismatch: expected object but found integer\"",
          "description": "Similar to the previous case but with a numeric root node, confirming that non‑object roots are correctly rejected."
        }
      ]
    }
  ]
}