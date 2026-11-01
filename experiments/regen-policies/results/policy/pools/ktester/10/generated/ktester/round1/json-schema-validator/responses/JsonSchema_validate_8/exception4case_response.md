{
  "new_test_cases": [
    {
      "group": "Malformed $id Percent Encoding – IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema associated with the JsonSchema instance contains an `$id` value with malformed percent‑encoding (e.g. \"http://example.com/schema%ZZ\"). During validation the method attempts to URL‑decode the `$id`, which triggers `IllegalArgumentException`. The test verifies that the exception propagates unchanged."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setValidateAll(true)"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Same malformed `$id` scenario but with a customizer that sets a flag. The customizer should be applied before the `$id` decoding step, and the resulting `IllegalArgumentException` must still be thrown."
        }
      ]
    },
    {
      "group": "Malformed $ref URI – IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "refValidJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema contains a `$ref` whose value is not a valid URI (e.g. \"#/definitions/Invalid%GRef\"). When the validator resolves the reference it calls `URLDecoder.decode`, which throws `IllegalArgumentException`. The test ensures the exception bubbles up."
        }
      ]
    },
    {
      "group": "Invalid Schema Definition – IllegalStateException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalStateException",
          "description": "The schema JSON node has duplicate keys (e.g., two \"type\" properties). The `JsonSchema.from` method detects this inconsistency and throws `IllegalStateException`. The test validates that the `validate` method propagates this exception."
        }
      ]
    },
    {
      "group": "OutputFormat Returns Null – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "NullReturningOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "A custom `OutputFormat` implementation deliberately returns `null` from its `apply` method. The `validate` method expects a non‑null result and therefore throws `NullPointerException`. This case confirms that the null‑return contract is enforced."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer Throws Checked Exception – RuntimeException Wrapper",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "validJsonNode"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat.INSTANCE"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new java.io.IOException(\"IO failure in customizer\"); }"
            }
          ],
          "expected": "RuntimeException",
          "description": "The customizer throws a checked `IOException`. Because `validate` does not declare checked exceptions, the implementation wraps it in a `RuntimeException`. The test asserts that a `RuntimeException` (with the original cause) is propagated."
        }
      ]
    }
  ]
}