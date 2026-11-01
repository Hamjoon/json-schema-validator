{
  "testCases": [
    {
      "group": "validate – normal flow (no customizer, no exception)",
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
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "format.customize(executionContext, validationContext) is invoked; executionCustomizer is not invoked; internal validate(executionContext,node) returns a Set<ValidationMessage> called expectedMessages; format.format(this, expectedMessages, executionContext, validationContext) is invoked and its return value is returned as the method result.",
          "description": "Verifies the happy‑path where no ExecutionContextCustomizer is supplied, validation succeeds without throwing, and the OutputFormat receives the correct validation messages."
        }
      ]
    },
    {
      "group": "validate – with ExecutionContextCustomizer",
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
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "format.customize(executionContext, validationContext) is invoked; executionCustomizer.customize(executionContext, validationContext) is also invoked exactly once; internal validate returns expectedMessages; format.format(this, expectedMessages, executionContext, validationContext) is invoked and its return value is returned.",
          "description": "Ensures that when a non‑null ExecutionContextCustomizer is supplied it is called after the format customization and before validation proceeds."
        }
      ]
    },
    {
      "group": "validate – FailFastAssertionException handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "invalidJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "format.customize(executionContext, validationContext) is invoked; internal validate throws FailFastAssertionException containing a Set<ValidationMessage> called failFastMessages; the catch block captures the exception and assigns validationMessages = failFastMessages; format.format(this, failFastMessages, executionContext, validationContext) is invoked and its return value is returned.",
          "description": "Tests the branch where the underlying validation throws FailFastAssertionException; the method must swallow the exception, extract its messages, and forward them to the OutputFormat."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext"
            },
            {
              "parameter": "node",
              "value": "invalidJsonNode"
            },
            {
              "parameter": "format",
              "value": "mockOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "mockExecutionCustomizer"
            }
          ],
          "expected": "format.customize(executionContext, validationContext) is invoked; executionCustomizer.customize(executionContext, validationContext) is invoked; internal validate throws FailFastAssertionException with failFastMessages; catch block assigns validationMessages = failFastMessages; format.format(this, failFastMessages, executionContext, validationContext) is invoked and its return value is returned.",
          "description": "Combines both a non‑null ExecutionContextCustomizer and a FailFastAssertionException to verify that customizer execution occurs before the exception is caught and that the correct messages are still passed to the OutputFormat."
        }
      ]
    }
  ]
}