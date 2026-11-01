{
  "testCases": [
    {
      "group": "Customization and Validation Flow",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "format.format(...) is called with the Set of ValidationMessage returned by JsonSchema.validate(executionContext, node). The returned value of format.format is propagated as the method result.",
          "description": "No ExecutionContextCustomizer is supplied; the method should invoke format.customize, skip the customizer block, call the internal validate method, and return the formatted output."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": "mockExecutionCustomizer" }
          ],
          "expected": "Both format.customize and executionCustomizer.customize are invoked exactly once. The result of format.format is returned.",
          "description": "A non‑null ExecutionContextCustomizer is provided; the method must call its customize method after format.customize, then proceed with validation and formatting."
        }
      ]
    },
    {
      "group": "FailFast Assertion Exception Handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "invalidJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "When JsonSchema.validate(executionContext, node) throws FailFastAssertionException, the catch block extracts its ValidationMessage set and passes it to format.format. The method returns the formatted result based on those messages.",
          "description": "Simulate a validation that aborts via FailFastAssertionException; the method must recover the messages from the exception and still invoke format.format."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "invalidJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": "mockExecutionCustomizer" }
          ],
          "expected": "Both format.customize and executionCustomizer.customize are called before the validation throws FailFastAssertionException. The caught messages are forwarded to format.format, and its return value is produced.",
          "description": "Combine a non‑null customizer with a validation that fails fast; ensures the customizer is still executed before the exception is caught and handled."
        }
      ]
    }
  ]
}