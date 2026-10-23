{
  "testCases": [
    {
      "group": "Empty validators map – returns empty set",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/\"" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "returned Set is empty and no pre‑walk or post‑walk listener methods are invoked",
          "description": "When the schema contains no validators (getValidators() returns an empty map) the walk method should finish immediately, returning an empty ValidationMessage set and never calling any listener."
        }
      ]
    },
    {
      "group": "Mixed pre‑walk outcomes – only true entries contribute messages",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/mixed\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "validationMessages contains only the messages from walkers whose pre‑walk listener returned true (e.g., \"msg‑from‑walker‑A\"), walkers with pre‑walk false contribute nothing; post‑walk listener called for each validator entry",
          "description": "The schema has two validators: A (pre‑walk true) and B (pre‑walk false). The walk method must invoke the walker for A, skip B, and finally return the union of messages from A only."
        }
      ]
    },
    {
      "group": "Pre‑walk listener throws RuntimeException – post‑walk still executed",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/exception\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "post‑walk listener’s runPostWalkListeners is invoked once for the validator entry, then the RuntimeException from the pre‑walk listener propagates",
          "description": "If keywordWalkListenerRunner.runPreWalkListeners throws an exception, the finally block must still execute, calling the post‑walk listener before the exception bubbles up."
        }
      ]
    },
    {
      "group": "Post‑walk listener throws RuntimeException – exception propagates after call",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/post‑exception\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "RuntimeException from post‑walk listener propagates out of walk method; any ValidationMessage(s) returned by the walker (if pre‑walk was true) are still added to the result set before the exception",
          "description": "When the post‑walk listener throws, the exception should not be swallowed; it must be re‑thrown after the finally block finishes. Messages collected before the exception must still be present in the returned set (if the method returns before the exception is thrown, the test can verify the exception is thrown)."
        }
      ]
    },
    {
      "group": "Null executionContext – listeners receive null without NPE",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": null },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/null‑ctx\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "walk proceeds; pre‑walk and post‑walk listeners are invoked with a null executionContext; no NullPointerException is thrown",
          "description": "The method should not assume executionContext is non‑null; it must forward the null value to listeners and walkers, allowing them to handle it."
        }
      ]
    },
    {
      "group": "shouldValidateSchema = false – walker still invoked",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/no‑validate\"" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "validationMessages contains the messages returned by the walker (e.g., \"msg‑from‑walker\"), proving that the flag does not prevent walker execution; post‑walk listener is still called",
          "description": "The boolean flag is only passed to the walker; the walk method itself does not branch on it. This test confirms that setting it to false does not skip the walker."
        }
      ]
    }
  ]
}