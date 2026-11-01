{
  "testCases": [
    {
      "group": "Boundary Numeric Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"value\": 0 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (value equals minimum inclusive)",
          "description": "Schema defines a numeric property \"value\" with minimum = 0 (inclusive) and maximum = 100 (inclusive). The instance uses the lower boundary value 0, which should be accepted."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"value\": 100 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (value equals maximum inclusive)",
          "description": "Same schema as above; the instance uses the upper boundary value 100, which should also be accepted."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"value\": -0.0001 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for 'value' below minimum",
          "description": "Instance value is just below the minimum (0). The validator must report a single validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"value\": 100.01 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for 'value' above maximum",
          "description": "Instance value is just above the maximum (100). The validator must report a single validation error."
        }
      ]
    },
    {
      "group": "Enum and Pattern Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"status\": \"ACTIVE\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (status matches allowed enum)",
          "description": "Schema defines \"status\" with enum [\"ACTIVE\", \"INACTIVE\", \"PENDING\"]. The instance supplies a valid enum value."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"status\": \"UNKNOWN\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for enum mismatch on 'status'",
          "description": "Instance supplies a value not present in the enum list; validator must emit an error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"email\": \"john.doe@example.com\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (email matches regex pattern)",
          "description": "Schema defines \"email\" with a regex pattern for a typical email address. The instance matches the pattern."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"email\": \"invalid-email@@example..com\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for pattern mismatch on 'email'",
          "description": "Instance email does not satisfy the regex pattern; validator must report a pattern error."
        }
      ]
    },
    {
      "group": "Array Constraints",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"tags\": [\"red\", \"green\", \"blue\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (array length within min/max and items valid)",
          "description": "Schema defines \"tags\" as an array with minItems = 1, maxItems = 5, and each item must be a string matching the enum [\"red\",\"green\",\"blue\",\"yellow\"]. The instance satisfies all constraints."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"tags\": [] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for 'tags' array below minItems",
          "description": "Array is empty while schema requires at least one item."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"tags\": [\"red\", \"purple\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for enum mismatch on second array element",
          "description": "Second element \"purple\" is not part of the allowed enum; validator must report an error for that element."
        }
      ]
    },
    {
      "group": "AdditionalProperties and $ref Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"person\": { \"name\": \"Sam\", \"age\": 40 } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (valid $ref to Person schema, no extra properties)",
          "description": "Root schema contains a property \"person\" that $ref's an external \"Person\" schema (with required name and age). Instance conforms exactly."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"person\": { \"name\": \"Sam\", \"age\": 40, \"extra\": \"value\" } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for disallowed additional property 'extra'",
          "description": "Person schema sets \"additionalProperties\": false. The instance adds an unexpected property, which should be flagged."
        }
      ]
    },
    {
      "group": "Locale / MessageSource Customization",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": -1 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setLocale(java.util.Locale.FRENCH)"
            }
          ],
          "expected": "Set containing ValidationMessage with French language text",
          "description": "The schema requires \"age\" to be >= 0. By setting the locale to French via the customizer, the returned ValidationMessage should be localized (e.g., \"doit être supérieur ou égal à 0\")."
        }
      ]
    },
    {
      "group": "Recursive Anchor Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"node\": { \"node\": { \"value\": \"leaf\" } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (recursive anchor resolves correctly)",
          "description": "Schema defines a recursive anchor \"$recursiveAnchor\": true and a property \"node\" that references the same schema. The deeply nested instance should validate without stack overflow."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"node\": { \"node\": { \"value\": 123 } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for type mismatch on 'value'",
          "description": "Same recursive schema, but the deepest \"value\" is a number while schema expects a string. Validator must report the type error."
        }
      ]
    },
    {
      "group": "ExecutionContext Attribute Ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"Eva\", \"age\": 28 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"unusedFlag\", true)"
            }
          ],
          "expected": "Empty Set (no validation errors)",
          "description": "Custom attribute \"unusedFlag\" is not consulted by any validator. Validation should behave exactly as if no attribute were set."
        }
      ]
    }
  ]
}