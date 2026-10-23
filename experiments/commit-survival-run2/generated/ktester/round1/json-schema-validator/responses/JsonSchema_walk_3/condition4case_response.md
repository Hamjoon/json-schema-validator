{
  "testCases": [
    {
      "group": "Pre‑walk listener returns true – walker is executed",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/myPath\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "validationMessages contains the ValidationMessage(s) returned by the JsonSchemaWalker (e.g., \"msg-from-walker\")",
          "description": "The pre‑walk listeners return true, therefore the JsonSchemaWalker.walk method is invoked. Its returned ValidationMessage set must be added to the final result."
        }
      ]
    },
    {
      "group": "Pre‑walk listener returns false – walker is skipped",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/myPath\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "validationMessages is empty (no messages added by the walker)",
          "description": "When the pre‑walk listeners return false, the JsonSchemaWalker.walk method must NOT be called. Consequently, the result set stays empty (apart from any post‑walk side‑effects)."
        }
      ]
    },
    {
      "group": "Post‑walk listener is always executed",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/myPath\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "post‑walk listener’s runPostWalkListeners method is invoked exactly once for each validator entry, regardless of the pre‑walk result",
          "description": "The finally block must run after the if‑statement, so the post‑walk listener is called even when the pre‑walk listener returns false."
        }
      ]
    },
    {
      "group": "Exception thrown by walker – post‑walk still runs",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/myPath\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "post‑walk listener is invoked before the RuntimeException propagates; the test should verify the exception is thrown and post‑walk was called",
          "description": "If JsonSchemaWalker.walk throws an exception, the finally block must still execute, invoking the post‑walk listener. The exception should bubble up after the finally block."
        }
      ]
    },
    {
      "group": "Multiple validators – each processed independently",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/myPath\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "validationMessages contains the union of messages returned by all walkers whose pre‑walk listeners returned true; post‑walk listener called for each validator entry",
          "description": "When the schema has more than one validator entry, the walk method must iterate over the entire map, applying pre‑walk, walker (if allowed), and post‑walk for each entry."
        }
      ]
    }
  ]
}