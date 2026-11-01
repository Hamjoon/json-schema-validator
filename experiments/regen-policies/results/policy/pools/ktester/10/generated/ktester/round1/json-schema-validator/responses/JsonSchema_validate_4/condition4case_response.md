{
  "testCases": [
    {
      "group": "Normal validation – valid JSON, default customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } }, \"required\": [\"name\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (no validation errors)",
          "description": "A well‑formed JSON instance that satisfies the schema. The customizer is null, so the method should behave like the overload without a customizer and return an empty validation result."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } }, \"required\": [\"age\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> { /* no modifications */ }"
            }
          ],
          "expected": "Set<ValidationMessage> is empty",
          "description": "Same as above but with an explicit no‑op customizer. The method must invoke the overload with the provided Consumer and still return an empty set."
        }
      ]
    },
    {
      "group": "Validation failures – schema violations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } }, \"required\": [\"id\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> contains at least one message about missing required property \"id\"",
          "description": "The instance does not contain the required property defined by the schema, so the validator must return a non‑empty set with an appropriate message."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"price\": { \"type\": \"number\" } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.setFailFast(true)"
            }
          ],
          "expected": "Set<ValidationMessage> contains exactly one message (fail‑fast mode)",
          "description": "A customizer forces fail‑fast validation. The instance violates the schema (price is missing but not required, but suppose another rule like \"minimum\": 0 is added in the real schema). The validator should stop after the first error, returning a single message."
        }
      ]
    },
    {
      "group": "Null / illegal arguments",
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
          "expected": "IllegalArgumentException (or NullPointerException) is thrown",
          "description": "Passing a null JsonNode is not allowed; the method should validate its arguments and throw an exception."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failure\" is propagated",
          "description": "If the provided ExecutionContextCustomizer itself throws, the validate method must not swallow the exception; it should propagate it to the caller."
        }
      ]
    },
    {
      "group": "Customizer side‑effects – context manipulation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\", \"pattern\": \"^[A-Z]{3}$\" } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.setEnabledFeatures(Set.of(\"PATTERN_PROPERTIES\"))"
            }
          ],
          "expected": "Set<ValidationMessage> is empty when instance matches pattern, non‑empty otherwise",
          "description": "The customizer enables a specific validation feature (e.g., pattern properties). The test should verify that the validator respects the modified execution context."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\", \"pattern\": \"^[A-Z]{3}$\" } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.setEnabledFeatures(Collections.emptySet())"
            }
          ],
          "expected": "Set<ValidationMessage> is empty regardless of pattern mismatch",
          "description": "When the customizer disables the pattern‑checking feature, the validator should skip that rule, resulting in no errors even if the instance does not match the pattern."
        }
      ]
    }
  ]
}