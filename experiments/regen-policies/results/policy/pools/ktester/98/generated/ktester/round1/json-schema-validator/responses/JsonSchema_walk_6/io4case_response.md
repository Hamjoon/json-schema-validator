{
  "testCases": [
    {
      "group": "walk_validNodeValidateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with empty error set (instance is valid against schema)",
          "description": "When the schema expects an object with a required string property \"name\", a matching JSON node is supplied and validation is enabled, the walk method should return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "walk_invalidNodeValidateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"age\":30}"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage indicating missing required property 'name'",
          "description": "When the instance does not satisfy the schema (required property missing) and validation is turned on, walk should return a ValidationResult with the appropriate error."
        }
      ]
    },
    {
      "group": "walk_validNodeValidateFalse",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"age\":30}"
            },
            {
              "parameter": "validate",
              "value": "false"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with empty error set (validation skipped)",
          "description": "When validation flag is false, walk must skip schema checks and always return a successful ValidationResult regardless of node content."
        }
      ]
    },
    {
      "group": "walk_withExecutionCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"name\":\"Alice\"}"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")"
            }
          ],
          "expected": "ValidationResult with empty error set and executionContext containing attribute 'custom'='value'",
          "description": "The consumer should be invoked before validation; after walk returns, the ExecutionContext must contain the attribute set by the customizer."
        }
      ]
    },
    {
      "group": "walk_nullNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult containing a ValidationMessage about null instance node",
          "description": "Passing a null JsonNode should result in a ValidationResult that reports the instance is missing (as per JSON Schema spec)."
        }
      ]
    },
    {
      "group": "walk_recursiveAnchorTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"self\":{}} (recursive reference)"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with empty error set (recursive anchor handled without infinite loop)",
          "description": "Schema contains a $recursiveAnchor and the instance references itself; walk must correctly resolve the anchor and finish validation without stack overflow."
        }
      ]
    },
    {
      "group": "walk_schemaReferenceResolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"refProp\":\"value\"}"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult with empty error set (referenced subschema validated successfully)",
          "description": "The schema uses $ref to another subschema that requires property \"refProp\" to be a string. The supplied node satisfies the subschema, so walk should succeed."
        }
      ]
    },
    {
      "group": "walk_executionCustomizerThrowsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing {\"name\":\"Bob\"}"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException propagated from executionCustomizer",
          "description": "If the consumer throws an exception, walk should not swallow it; the exception must propagate to the caller."
        }
      ]
    }
  ]
}