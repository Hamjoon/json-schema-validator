{
  "testCases": [
    {
      "group": "Null Checks",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "NullPointerException (executionContext must not be null)",
          "description": "Validate that the method throws NPE when the ExecutionContext argument is null."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "NullPointerException (node must not be null)",
          "description": "Validate that the method throws NPE when the JsonNode argument is null."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "nonNullJsonNode"
            },
            {
              "parameter": "format",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (format must not be null)",
          "description": "Validate that the method throws NPE when the OutputFormat argument is null."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "formattedResult (no customizer applied)",
          "description": "When the fourth parameter (executionCustomizer) is null, the validator should run with the default execution context and return the formatted result."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockCustomizerThatAddsFlag"
            }
          ],
          "expected": "formattedResult (customizer applied, flag set)",
          "description": "When a non‑null ExecutionContextCustomizer is supplied, the method must invoke it before validation; the test verifies that the customizer’s side‑effect (e.g., a flag in the context) is observed in the formatted output."
        }
      ]
    },
    {
      "group": "Validation Success Path",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNodeConformingToSchema"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormatReturningList"
            }
          ],
          "expected": "emptyList (no validation messages)",
          "description": "When the supplied JsonNode satisfies the schema, the validator should produce an empty collection of ValidationMessage objects, which the OutputFormat turns into an empty list."
        }
      ]
    },
    {
      "group": "Validation Failure Path",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "jsonNodeViolatingSchema"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormatReturningList"
            }
          ],
          "expected": "listContainingAtLeastOneMessage (validation error messages)",
          "description": "When the JsonNode does not conform to the schema, the validator must collect ValidationMessage instances; the OutputFormat should return a list containing those messages."
        }
      ]
    },
    {
      "group": "Suppress Sub‑Schema Retrieval Branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithSuppressTrue"
            },
            {
              "parameter": "node",
              "value": "jsonNodeRequiringSubSchema"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "formattedResult (sub‑schema not fetched, validation proceeds with current schema only)",
          "description": "If the validator instance was constructed with `suppressSubSchemaRetrieval = true`, the method must skip `fetchSubSchemaNode` and validate only against the current schema. The test confirms that no SubSchema‑related messages appear."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithSuppressFalse"
            },
            {
              "parameter": "node",
              "value": "jsonNodeRequiringSubSchema"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "formattedResult (sub‑schema fetched and validation applied)",
          "description": "When `suppressSubSchemaRetrieval = false`, the validator should call `fetchSubSchemaNode` and include any errors from the retrieved sub‑schema in the formatted output."
        }
      ]
    },
    {
      "group": "ApplyDefaultsStrategy Branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithStrategyNone"
            },
            {
              "parameter": "node",
              "value": "jsonNodeMissingOptionalProperties"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "formattedResult (no defaults applied, missing optional properties remain absent)",
          "description": "When the validator is created with `ApplyDefaultsStrategy.NONE`, the method must not modify the input node; the result should reflect the original node."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContextWithStrategyInsert"
            },
            {
              "parameter": "node",
              "value": "jsonNodeMissingOptionalProperties"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            }
          ],
          "expected": "formattedResult (defaults inserted into node before validation)",
          "description": "With `ApplyDefaultsStrategy.INSERT`, the validator should add missing default values to the node prior to validation; the formatted output should indicate a successful validation after defaults are applied."
        }
      ]
    }
  ]
}