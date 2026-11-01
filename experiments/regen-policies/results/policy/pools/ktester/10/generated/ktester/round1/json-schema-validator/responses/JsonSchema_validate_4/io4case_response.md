{
  "tests": [
    {
      "group": "Multiple violations – failFast disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\", \"minimum\": 0 }, \"code\": { \"type\": \"string\", \"pattern\": \"^[A-Z]{3}$\" } }, \"required\": [\"age\", \"code\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> contains exactly two messages – one for \"age\" below minimum and one for \"code\" not matching the pattern",
          "description": "The instance violates two independent constraints and the default execution context does not enable fail‑fast, so both errors must be reported."
        }
      ]
    },
    {
      "group": "Type mismatch – customizer disables type checking",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"integer\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.setEnabledFeatures(Collections.emptySet())"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (type validator is disabled by the customizer)",
          "description": "A customizer clears all enabled features, effectively turning off the type validator. Even though the instance is a string, no validation error should be produced."
        }
      ]
    },
    {
      "group": "Array schema validation – valid case",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (all array items satisfy the integer type)",
          "description": "The schema expects an array of integers and the instance provides a correct array; validation must succeed."
        }
      ]
    },
    {
      "group": "Array schema validation – invalid case",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> contains a message about element at index 1 not being an integer",
          "description": "The instance contains a non‑integer element; the validator must report the type error for that specific array index."
        }
      ]
    },
    {
      "group": "Reference resolution – $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"$ref\": \"#/definitions/id\", \"definitions\": { \"id\": { \"type\": \"integer\" } } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> contains a type‑mismatch message because the instance is a string while the referenced schema expects an integer",
          "description": "The validator must resolve the $ref correctly and apply the referenced integer type constraint."
        }
      ]
    },
    {
      "group": "Custom format validator – customizer registers format",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"custom-email\" } }, \"required\": [\"email\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.addFormatValidator(\"custom-email\", (value, node) -> value.matches(\"^[^@]+@[^@]+\\\\.[^@]+$\") ? Optional.empty() : Optional.of(\"Invalid custom email\"))"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (the custom format validator accepts the provided email)",
          "description": "A custom format validator is registered via the customizer; the instance complies with that validator, so no error is reported."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"custom-email\" } }, \"required\": [\"email\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.addFormatValidator(\"custom-email\", (value, node) -> Optional.of(\"Invalid custom email\"))"
            }
          ],
          "expected": "Set<ValidationMessage> contains exactly one message \"Invalid custom email\"",
          "description": "The same custom format is registered but deliberately fails; the validator must surface the custom error message."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer invocation – side‑effect verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } }, \"required\": [\"id\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> SideEffectHolder.invoked = true"
            }
          ],
          "expected": "Set<ValidationMessage> is empty and SideEffectHolder.invoked == true after validation",
          "description": "The test asserts that the provided customizer is actually executed (by toggling a static flag) and that normal validation still succeeds."
        }
      ]
    },
    {
      "group": "Locale/message customization – customizer sets locale",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"type\": \"object\", \"required\": [\"name\"] }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "execution -> execution.setLocale(java.util.Locale.FRENCH)"
            }
          ],
          "expected": "Set<ValidationMessage> contains a message whose text is in French (e.g., \"la propriété requise 'name' est manquante\")",
          "description": "When the customizer changes the locale, the validator must produce messages in the selected language."
        }
      ]
    },
    {
      "group": "Empty schema – no constraints",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (an empty schema imposes no validation rules)",
          "description": "A schema without any keywords should never produce validation errors regardless of the instance content."
        }
      ]
    }
  ]
}