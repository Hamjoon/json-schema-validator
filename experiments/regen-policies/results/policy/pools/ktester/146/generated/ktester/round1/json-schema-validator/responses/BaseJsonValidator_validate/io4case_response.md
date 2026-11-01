{
  "groups": [
    {
      "group": "Sub‑schema retrieval edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(suppressSubSchemaRetrieval=false, collectAnnotations=false, fetchSubSchemaNodeResult=null)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ObjectNode)"
            }
          ],
          "expected": "empty Set<ValidationMessage> (no sub‑schema fetched, therefore no additional errors)",
          "description": "When `suppressSubSchemaRetrieval` is false but `fetchSubSchemaNode` returns null, the validator should simply return the result of its own keyword validation (which is empty for a valid node)."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(suppressSubSchemaRetrieval=false, collectAnnotations=false, fetchSubSchemaNodeResult=mockSubSchemaNode(errors=[\"sub‑schema violation\"]))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ObjectNode)"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"sub‑schema violation\"",
          "description": "When `suppressSubSchemaRetrieval` is false and `fetchSubSchemaNode` returns a sub‑schema that itself produces a validation error, that error must be propagated to the caller."
        }
      ]
    },
    {
      "group": "Discriminator missing property",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(withDiscriminator=true, discriminatorProperty=\"type\", discriminatorValue=null)"
            }
          ],
          "expected": "Set containing a ValidationMessage indicating missing discriminator property \"type\"",
          "description": "If a node contains a discriminator object but the required discriminator property is absent, `checkDiscriminatorMatch` should generate a ValidationMessage describing the missing property."
        }
      ]
    },
    {
      "group": "Non‑Object node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ArrayNode)"
            }
          ],
          "expected": "Set containing a ValidationMessage with message \"expected type ObjectNode but found ArrayNode\"",
          "description": "When the validator expects an ObjectNode but receives an ArrayNode, it must report a type‑mismatch ValidationMessage."
        }
      ]
    },
    {
      "group": "Annotation without keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotations=true, keyword=null)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode()"
            }
          ],
          "expected": "no annotation added to ExecutionContext (keyword is null, so annotation is skipped)",
          "description": "When `collectAnnotations` returns true but the keyword argument is null, the validator should not create a JsonNodeAnnotation."
        }
      ]
    },
    {
      "group": "Exception propagation from ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(collectAnnotationsThrows=RuntimeException(\"boom\"))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ObjectNode)"
            }
          ],
          "expected": "RuntimeException with message \"boom\" propagated",
          "description": "If `ExecutionContext.collectAnnotations` throws an unchecked exception, `validate` must not swallow it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "Debug logging activation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(loggerDebugEnabled=true)"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(valid=true, type=ObjectNode)"
            }
          ],
          "expected": "empty Set<ValidationMessage> and BaseJsonValidator.debug invoked once",
          "description": "When the logger is debug‑enabled, the static `debug` method should be called during validation; the validation result itself remains unchanged (empty set for a valid node)."
        }
      ]
    }
  ]
}