{
  "groups": [
    {
      "group": "Empty validators list",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (no validators registered)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode (same as node)" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath (root)" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "empty Set – walk iterates over an empty validator collection and returns no errors; no pre‑walk or post‑walk listeners are invoked.",
          "description": "When the schema has no validators (getValidators() returns an empty list), walk should simply return an empty error set without invoking any listeners."
        }
      ]
    },
    {
      "group": "Validator throws exception",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (pre‑walk listener returns true)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "RuntimeException propagated; post‑walk listener executed once despite the exception; errors set remains empty because the validator never returned a result.",
          "description": "If a validator’s walk method throws a RuntimeException, the exception should bubble up, but the outer finally block must still run the post‑walk listener. No error messages are added because the validator did not produce a result."
        }
      ]
    },
    {
      "group": "Pre‑walk listener throws exception",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (pre‑walk listener throws RuntimeException)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "RuntimeException propagated; post‑walk listener executed once; validator.walk never invoked; errors set empty.",
          "description": "When the pre‑walk listener throws, the exception propagates out of the if‑statement, but the outer finally ensures the post‑walk listener is still called. No validator walk occurs."
        }
      ]
    },
    {
      "group": "Duplicate ValidationMessage handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (two validators, both return the same ValidationMessage instance)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "Set size = 1 – LinkedHashSet removes duplicate ValidationMessage objects; both pre‑walk and post‑walk listeners invoked for each validator.",
          "description": "Validators A and B each return a Set containing an identical ValidationMessage (same messageId, path, etc.). Because walk aggregates results into a LinkedHashSet, the final error set must contain only one instance."
        }
      ]
    },
    {
      "group": "Order preservation of error messages",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (three validators, each returns a distinct ValidationMessage)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "LinkedHashSet preserves insertion order: [msgFromValidatorA, msgFromValidatorB, msgFromValidatorC]; all listeners invoked in the same order as validators.",
          "description": "Validate that walk does not reorder messages; the order of messages in the returned Set must reflect the order in which validators were iterated."
        }
      ]
    },
    {
      "group": "shouldValidateSchema flag false but validator still executed when pre‑walk allows",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mocked ExecutionContext (pre‑walk listener returns true)" },
            { "parameter": "node", "value": "mocked JsonNode" },
            { "parameter": "rootNode", "value": "mocked JsonNode" },
            { "parameter": "instanceLocation", "value": "mocked JsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "validator.walk called with shouldValidateSchema = false; any returned messages are added to errors set; post‑walk listener invoked.",
          "description": "The shouldValidateSchema flag is passed through to each validator. Even when false, the validator is still invoked (pre‑walk allowed). Verify that the flag value is correctly forwarded and that messages are still collected."
        }
      ]
    }
  ]
}