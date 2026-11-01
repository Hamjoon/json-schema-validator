{
  "testCases": [
    {
      "group": "Discriminator Handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithDiscriminator" },
            { "parameter": "node", "value": "jsonNodeWithDiscriminatorPropertyMatching" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (discriminator matched, no validation errors)",
          "description": "When the schema contains a discriminator and the instance property value matches a defined mapping, `checkDiscriminatorMatch` should succeed and validation should produce no messages."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithDiscriminator" },
            { "parameter": "node", "value": "jsonNodeWithDiscriminatorPropertyMismatching" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "listContainingDiscriminatorMismatchMessage",
          "description": "If the discriminator property value does not correspond to any mapping, `checkDiscriminatorMatch` must add a ValidationMessage indicating the mismatch."
        }
      ]
    },
    {
      "group": "Debug Logging",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithDebugEnabled" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" }
          ],
          "expected": "formattedResult (debug log emitted, validation passes)",
          "description": "When the logger is set to DEBUG level, the static `debug` method should be invoked; the test verifies that the method still returns the correctly formatted result."
        }
      ]
    },
    {
      "group": "OutputFormat Exception Propagation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormatThatThrows" }
          ],
          "expected": "RuntimeException (propagated from OutputFormat)",
          "description": "If the supplied OutputFormat throws an exception while processing the ValidationMessage set, the validator must not swallow it; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Exception",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" }
          ],
          "expected": "RuntimeException (customizer exception propagated)",
          "description": "When a non‑null ExecutionContextCustomizer throws an exception, the overloaded `validate(..., null, customizer)` path must let the exception bubble up; this test invokes the four‑argument version directly via reflection."
        }
      ]
    },
    {
      "group": "Number Equality Edge Cases",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNodeWithNaN" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (NaN compared using equals returns false, no false positive)",
          "description": "The static `equals(double,double)` must treat NaN values correctly (NaN != NaN). Validation that relies on numeric equality should therefore not produce a spurious error."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "jsonNodeWithPositiveZeroAndNegativeZero" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (0.0 equals -0.0)",
          "description": "Verify that `equals` treats +0.0 and -0.0 as equal, matching JSON Schema numeric comparison semantics."
        }
      ]
    },
    {
      "group": "Fetch Sub‑Schema Null Return",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithSuppressFalseAndNoSubSchema" },
            { "parameter": "node", "value": "jsonNodeRequiringSubSchema" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (no sub‑schema found, validator falls back to current schema only)",
          "description": "When `suppressSubSchemaRetrieval` is false but `fetchSubSchemaNode` returns null, the validator should continue using the current schema without throwing an exception."
        }
      ]
    },
    {
      "group": "ApplyDefaultsStrategy with Null Node",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithStrategyInsert" },
            { "parameter": "node", "value": "nullJsonNodeRepresentingMissingRoot" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "NullPointerException (node must not be null)",
          "description": "Even when defaults insertion is enabled, a null JsonNode should still trigger the initial null‑check before any default logic is applied."
        }
      ]
    },
    {
      "group": "Suppress Sub‑Schema Retrieval with Null Sub‑Schema",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithSuppressTrue" },
            { "parameter": "node", "value": "jsonNodeRequiringSubSchema" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (sub‑schema retrieval suppressed, validation succeeds)",
          "description": "When suppression is true, the validator must not attempt to fetch a sub‑schema; the test confirms that validation proceeds without errors even though a sub‑schema would normally be required."
        }
      ]
    },
    {
      "group": "ValidationContext Null Handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContextWithNullValidationContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" }
          ],
          "expected": "emptyList (validator creates a default ValidationContext internally)",
          "description": "If the `validationContext` field of the validator instance is null, the method should instantiate a default context and continue validation without NPE."
        }
      ]
    }
  ]
}