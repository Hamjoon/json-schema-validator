{
  "tests": [
    {
      "group": "Customizer modifies validation behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"price\": -5}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that disables the MinimumValidator (e -> e.getConfig().setValidateMin(false))"
            }
          ],
          "expected": "An empty Set<ValidationMessage> because the minimum‑value check has been disabled by the customizer, even though the JSON violates the schema's minimum constraint.",
          "description": "The schema defines a numeric property \"price\" with a minimum of 0. The customizer turns off minimum validation, so the negative value should not produce a validation error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"price\": -5}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that disables the RequiredValidator (e -> e.getConfig().setValidateRequired(false))"
            }
          ],
          "expected": "An empty Set<ValidationMessage> because the required‑field check has been turned off, allowing the missing \"price\" field to be ignored.",
          "description": "The schema marks \"price\" as required. By disabling the required validator via the customizer, the validation should succeed despite the missing field."
        }
      ]
    },
    {
      "group": "Internationalization of validation messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":123}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets MessageSource locale to French (e -> e.setMessageSource(MessageSource.forLocale(Locale.FRENCH)))"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message whose text is in French, e.g., \"Le type attendu est string mais la valeur fournie est integer\".",
          "description": "When the customizer switches the message source to French, validation errors should be reported in French rather than the default English."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":\"thirty\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets MessageSource to a custom bundle with key 'type' mapped to 'Tipo erróneo' (Spanish)"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message that includes the Spanish phrase \"Tipo erróneo\".",
          "description": "Custom message source overrides the default wording; the test verifies that the validator uses the supplied MessageSource."
        }
      ]
    },
    {
      "group": "Fail‑fast behavior via customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":123,\"age\":\"thirty\",\"email\":null}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that enables fail‑fast mode (e -> e.getConfig().setFailFast(true))"
            }
          ],
          "expected": "A Set<ValidationMessage> containing exactly one ValidationMessage (the first error encountered, e.g., type mismatch on \"name\").",
          "description": "When fail‑fast is enabled, validation should stop after the first failure, returning a single message instead of a collection of all errors."
        }
      ]
    },
    {
      "group": "Array schema handling with customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing [{\"id\":1},{\"id\":\"two\"}]"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "A non‑empty Set<ValidationMessage> containing a type‑mismatch error for the second array element's \"id\" property.",
          "description": "The schema expects an array of objects where \"id\" is an integer. The second element violates this, so a validation error should be reported."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing []"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that disables the MinItemsValidator (e -> e.getConfig().setValidateMinItems(false))"
            }
          ],
          "expected": "An empty Set<ValidationMessage> because the schema's \"minItems\":1 constraint is ignored by the customizer.",
          "description": "The schema requires at least one item. By disabling the min‑items validator, an empty array should pass validation."
        }
      ]
    },
    {
      "group": "Customizer‑induced exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that throws IllegalStateException(\"customizer failure\")"
            }
          ],
          "expected": "IllegalStateException with message \"customizer failure\" propagates out of validate method.",
          "description": "If the customizer throws an unchecked exception, the validate method must not catch it; the exception should bubble up to the caller."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets a null ValidationContext inside the ExecutionContext (e -> e.setValidationContext(null))"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown during validation because the required ValidationContext is missing.",
          "description": "Altering the ExecutionContext to an invalid state should cause validation to fail early with a clear exception."
        }
      ]
    }
  ]
}