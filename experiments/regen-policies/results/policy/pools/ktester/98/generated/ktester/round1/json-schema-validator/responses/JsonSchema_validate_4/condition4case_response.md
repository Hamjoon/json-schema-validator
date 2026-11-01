{
  "testCases": [
    {
      "group": "Null Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException",
          "description": "Both arguments are null – the method should reject null rootNode (or executionCustomizer) and raise a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "Throws NullPointerException",
          "description": "rootNode is null while a valid customizer is supplied – the method must still throw NPE because the JSON instance to validate is missing."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException",
          "description": "ExecutionContextCustomizer is null – the method should validate the presence of the customizer and throw NPE."
        }
      ]
    },
    {
      "group": "Valid Schema Validation (no validation errors)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\", \"age\": 30 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Empty Set (no ValidationMessage)",
          "description": "A JSON document that conforms to the schema – the validator should return an empty set of ValidationMessage."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"id\": \"12345\", \"active\": true }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")"
            }
          ],
          "expected": "Empty Set (no ValidationMessage)",
          "description": "Customizer mutates the ExecutionContext but does not affect validation outcome; still should return an empty set."
        }
      ]
    },
    {
      "group": "Invalid Schema Validation (validation errors)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": 123, \"age\": \"thirty\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing at least two ValidationMessage objects (type mismatch for 'name' and 'age')",
          "description": "JSON violates type constraints – the validator must report validation messages for each offending property."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set containing ValidationMessage for missing required property 'name'",
          "description": "Empty object fails required‑property validation; the returned set should contain a single message describing the missing field."
        }
      ]
    },
    {
      "group": "Custom ExecutionContext Effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"Alice\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "Set containing only the first ValidationMessage encountered (fail‑fast mode)",
          "description": "When the customizer enables fail‑fast, validation should stop after the first error; the result set size must be 1."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"Bob\", \"age\": -5 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"allowNegative\", false)"
            }
          ],
          "expected": "Set containing ValidationMessage for 'age' being negative",
          "description": "Customizer adds a flag that the validator reads to reject negative numbers; the resulting set should contain the appropriate message."
        }
      ]
    }
  ]
}