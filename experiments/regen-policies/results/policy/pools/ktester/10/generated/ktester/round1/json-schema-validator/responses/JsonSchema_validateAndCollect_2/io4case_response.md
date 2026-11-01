{
  "testCases": [
    {
      "group": "$ref Resolution to Internal Definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"person\": {\"$ref\": \"#/definitions/name\"}}"
            }
          ],
          "expected": "ValidationResult with no ValidationMessage because the $ref resolves to a definition that validates the referenced value (e.g., a string) correctly.",
          "description": "The schema contains a definition \"name\" of type string. The instance uses $ref to point to that definition. validateAndCollect should resolve the $ref and apply the referenced validators, resulting in a successful validation."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"person\": {\"$ref\": \"#/definitions/age\"}}"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating type mismatch (expected integer) because the referenced definition expects an integer but the instance provides an object.",
          "description": "The $ref resolves to a definition that expects an integer. The supplied node is an object, so the validator should report a type error after $ref resolution."
        }
      ]
    },
    {
      "group": "Enum Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"RED\""
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the value is one of the allowed enum values [\"RED\",\"GREEN\",\"BLUE\"].",
          "description": "The schema defines an enum of three colour strings. The instance supplies a matching value, so validation succeeds."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"YELLOW\""
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage stating that the value is not one of the allowed enum values.",
          "description": "The instance provides a value not present in the enum list, triggering an enum validation error."
        }
      ]
    },
    {
      "group": "Const Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "42"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the instance equals the const value defined in the schema.",
          "description": "The schema uses \"const\": 42. The instance matches exactly, so validation passes."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "43"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating that the instance does not equal the const value 42.",
          "description": "The instance differs from the const value, causing a const validation error."
        }
      ]
    },
    {
      "group": "String Pattern Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"AB1234\""
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the string matches the regex pattern \"^[A-Z]{2}\\d{4}$\".",
          "description": "The schema defines a pattern that requires two uppercase letters followed by four digits. The instance satisfies the pattern."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"ab1234\""
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating pattern mismatch.",
          "description": "The instance uses lowercase letters, violating the defined regex pattern."
        }
      ]
    },
    {
      "group": "Numeric Range Validation (minimum/maximum)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "15"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the number lies within the inclusive range [10,20].",
          "description": "The schema defines \"minimum\":10 and \"maximum\":20. The instance value 15 satisfies both constraints."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "9"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage stating that the value is less than the minimum of 10.",
          "description": "The instance is below the allowed minimum, triggering a minimum validation error."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "21"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage stating that the value exceeds the maximum of 20.",
          "description": "The instance exceeds the defined maximum, causing a maximum validation error."
        }
      ]
    },
    {
      "group": "Array Length Validation (minItems / maxItems)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[1,2,3]"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because array length (3) is between minItems=2 and maxItems=5.",
          "description": "The schema restricts array length to a minimum of 2 and a maximum of 5. The supplied array satisfies both constraints."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[1]"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating that the array has fewer items than the required minItems of 2.",
          "description": "Array length is below the allowed minimum, resulting in a validation error."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[1,2,3,4,5,6]"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating that the array exceeds the maxItems limit of 5.",
          "description": "Array length exceeds the defined maximum, causing a validation error."
        }
      ]
    },
    {
      "group": "AdditionalProperties False",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"allowed\": \"yes\"}"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the object contains only the defined property \"allowed\".",
          "description": "The schema defines a property \"allowed\" and sets \"additionalProperties\": false. The instance complies."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"allowed\": \"yes\", \"extra\": 123}"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage stating that property \"extra\" is not allowed (additionalProperties = false).",
          "description": "The instance contains an extra property not defined in the schema, which should be rejected."
        }
      ]
    },
    {
      "group": "Format Validation (date-time)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"2023-08-15T13:45:30Z\""
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the string conforms to the \"date-time\" format.",
          "description": "The schema specifies \"format\": \"date-time\". The supplied ISO‑8601 string is valid."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"15-08-2023 13:45\""
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating that the value does not match the \"date-time\" format.",
          "description": "The string does not follow the required date‑time format, leading to a format validation error."
        }
      ]
    },
    {
      "group": "Multiple Validators Combined (type + minLength + pattern)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"AB12\""
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the string is of type string, meets minLength=4, and matches pattern \"^[A-Z]{2}\\d{2}$\".",
          "description": "The schema applies three validators simultaneously. The instance satisfies all, resulting in no errors."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"A12\""
            }
          ],
          "expected": "ValidationResult containing two ValidationMessages: one for minLength violation (length 3 < 4) and one for pattern mismatch.",
          "description": "The instance fails both the minimum length and pattern validators, so both errors should be reported."
        }
      ]
    },
    {
      "group": "Keyword Walk Listener Runner Null (No Invocation)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"simple\": true}"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set and no interaction with WalkListenerRunner (i.e., no NPE, listener not called).",
          "description": "When keywordWalkListenerRunner is null, validateAndCollect should execute normally without attempting to invoke a listener."
        }
      ]
    },
    {
      "group": "$id Base URI Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"ref\": {\"$ref\": \"other-schema.json#/defs/value\"}}"
            }
          ],
          "expected": "ValidationResult with empty ValidationMessage set because the $id of the current schema resolves the relative reference correctly.",
          "description": "The schema defines an \"$id\": \"http://example.com/schemas/\". The instance uses a relative $ref. Validation should resolve the reference using the base URI derived from $id and succeed."
        }
      ]
    },
    {
      "group": "ExecutionContext Customizer Adding Custom Format Validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"CUSTOM-123\""
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage produced by the custom format validator (e.g., \"custom-format\" error) while other validators pass.",
          "description": "A custom ExecutionContext adds a validator for a made‑up format \"custom-format\". The instance does not satisfy this format, so the custom validator should contribute an error to the result."
        }
      ]
    }
  ]
}