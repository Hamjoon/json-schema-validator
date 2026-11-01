{
  "newTestCases": [
    {
      "group": "ValidationContext Null with Retrieval Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "mockJsonSchema",
          "description": "When `suppressSubSchemaRetrieval` is false and a `null` ValidationContext is supplied, `fetchSubSchemaNode` should still delegate to `obtainSubSchemaNode` (passing the validator's `schemaNode` and `null`) and return whatever that method yields. The test mocks `obtainSubSchemaNode` to return a dummy `JsonSchema` and verifies the returned reference."
        }
      ]
    },
    {
      "group": "ObtainSubSchemaNode Invocation Verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "obtainSubSchemaNode called with validator.schemaNode and mockValidationContext",
          "description": "Ensures that `fetchSubSchemaNode` forwards the exact `schemaNode` held by the validator and the supplied `ValidationContext` to the private static `obtainSubSchemaNode`. The test uses a spy or PowerMockito `verifyStatic` to assert the method was invoked once with the expected arguments."
        }
      ]
    },
    {
      "group": "Repeated Calls Consistency",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "sameInstance",
          "description": "Calls `fetchSubSchemaNode` twice on the same validator instance (with `suppressSubSchemaRetrieval` = false) and expects both calls to return the identical `JsonSchema` object returned by `obtainSubSchemaNode`. This confirms no hidden state mutation between invocations."
        }
      ]
    },
    {
      "group": "Empty ValidationContext with Retrieval Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "emptyValidationContext"
            }
          ],
          "expected": "mockJsonSchema",
          "description": "Provides a freshly constructed `ValidationContext` that contains no custom configuration. With `suppressSubSchemaRetrieval` = false, `fetchSubSchemaNode` should still call `obtainSubSchemaNode` and return its mock result."
        }
      ]
    },
    {
      "group": "Suppress Flag True Overrides Non‑null SchemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "null",
          "description": "Even when the validator holds a valid, non‑null `schemaNode` and `suppressSubSchemaRetrieval` is true, `fetchSubSchemaNode` must short‑circuit and return `null`. This case differs from the existing ones by explicitly setting a non‑null `schemaNode` before invoking the method."
        }
      ]
    }
  ]
}