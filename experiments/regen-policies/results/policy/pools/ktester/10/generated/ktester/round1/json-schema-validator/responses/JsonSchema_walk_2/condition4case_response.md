{
  "group": "walk(JsonNode, boolean) - basic validation scenarios",
  "cases": [
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"name\": \"John\", \"age\": 30 }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.success – no ValidationMessage objects (the instance conforms to the schema)",
      "description": "When `validate` is true and the supplied JsonNode satisfies all schema constraints, walk should return a successful ValidationResult with an empty error set."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"name\": 123, \"age\": \"thirty\" }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.failure – contains at least two ValidationMessage objects (type mismatch for `name` and `age`)",
      "description": "When `validate` is true and the JsonNode violates the schema (wrong types), walk must collect the corresponding validation errors."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"unexpected\": \"field\" }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.failure – contains a ValidationMessage about additionalProperties not allowed",
      "description": "Tests the branch that handles extra properties when `validate` is true and `additionalProperties` is disallowed by the schema."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing any JSON (e.g., { \"any\": \"value\" })"
        },
        {
          "parameter": "validate",
          "value": false
        }
      ],
      "expected": "ValidationResult.success – empty error set (no validation performed)",
      "description": "When `validate` is false the method should skip all validator execution and immediately return a successful result regardless of the node content."
    },
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
      "expected": "ValidationResult.failure – contains a ValidationMessage indicating that the instance node is null or missing",
      "description": "Ensures the null‑node guard branch is exercised; the method must treat a null instance as a validation failure."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"name\": \"Alice\" }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.success – empty error set (required property `age` is missing but the schema has a `required` validator that is *not* present in this particular schema configuration)",
      "description": "Covers the branch where the schema does **not** contain a required‑field validator (hasRequiredValidator() returns false). The walk should succeed even though the instance is missing a property that would otherwise be required."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"type\": \"object\" }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.success – empty error set (type validator present and passes)",
      "description": "Triggers the branch where the schema *does* contain a type validator (hasTypeValidator() returns true) and the instance type matches the expected type."
    },
    {
      "input": [
        {
          "parameter": "node",
          "value": "ObjectNode representing { \"type\": 123 }"
        },
        {
          "parameter": "validate",
          "value": true
        }
      ],
      "expected": "ValidationResult.failure – contains a ValidationMessage about type mismatch",
      "description": "Executes the branch where a type validator is present but the instance type does not match, causing a validation error to be recorded."
    }
  ]
}