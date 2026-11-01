{
  "testCases": [
    {
      "group": "Null Parameter Handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": null },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException is thrown when format.customize is invoked with a null ExecutionContext. No validation or format.format calls are performed.",
          "description": "Verify that passing a null ExecutionContext causes the method to fail early with NPE during format.customize."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": null },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException is thrown because format is null before any method calls. The method should not attempt to call format.customize or format.format.",
          "description": "Ensure that a null OutputFormat leads to an immediate NPE, protecting against dereferencing a null reference."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": null },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "NullPointerException is thrown when the internal validate(executionContext, node) method receives a null JsonNode. format.format is never called.",
          "description": "Validate that a null JsonNode argument is not tolerated and results in an NPE from the core validation routine."
        }
      ]
    },
    {
      "group": "Empty Validation Result",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNodeWithNoErrors" },
            { "parameter": "format", "value": "mockOutputFormatReturningList" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "format.format(this, emptySet, executionContext, validationContext) is called and returns an empty List. The method returns that empty List.",
          "description": "When the schema validates successfully and returns an empty Set of ValidationMessage, the OutputFormat should receive the empty set and produce an empty collection as the final result."
        }
      ]
    },
    {
      "group": "Customizer Exception Propagation",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormat" },
            { "parameter": "executionCustomizer", "value": "exceptionThrowingCustomizer" }
          ],
          "expected": "RuntimeException thrown by executionCustomizer.customize propagates out of validate method. format.format is never invoked.",
          "description": "If the supplied ExecutionContextCustomizer throws an exception, the method must not swallow it; the exception should bubble up and validation/formatting should be aborted."
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
            { "parameter": "format", "value": "exceptionThrowingOutputFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "RuntimeException thrown by format.customize propagates out of validate method. No validation or format.format call occurs.",
          "description": "When the OutputFormat's customize method throws an exception, the method must forward that exception; internal validation must not be performed."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "exceptionThrowingOutputFormatDuringFormat" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "RuntimeException thrown by format.format propagates out of validate method after successful validation. The exception is not caught inside the method.",
          "description": "If format.format throws an exception after validation succeeds, the method should let the exception escape, preserving the original error context."
        }
      ]
    },
    {
      "group": "Null ValidationMessage Set Handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "validJsonNode" },
            { "parameter": "format", "value": "mockOutputFormatAcceptingNull" },
            { "parameter": "executionCustomizer", "value": null }
          ],
          "expected": "format.format(this, null, executionContext, validationContext) is called and its return value is propagated.",
          "description": "Simulate a scenario where the internal validate method returns null (e.g., custom schema implementation). The method must forward the null Set to the OutputFormat without throwing NPE."
        }
      ]
    }
  ]
}