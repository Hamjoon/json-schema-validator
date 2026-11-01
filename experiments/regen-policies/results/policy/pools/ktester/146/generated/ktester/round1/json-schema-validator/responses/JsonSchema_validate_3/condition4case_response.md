{
  "testCases": [
    {
      "group": "Null root node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "IllegalArgumentException with message \"rootNode must not be null\"",
          "description": "When the supplied JsonNode is null the method should reject the call and throw an IllegalArgumentException before invoking any customizer."
        }
      ]
    },
    {
      "group": "Null consumer handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> that is empty (validation succeeds)",
          "description": "If the consumer is null the method should behave as if an empty customizer was supplied; the schema (a simple schema requiring a string property \"name\") validates the provided node successfully."
        }
      ]
    },
    {
      "group": "Consumer customizes ExecutionContext – success path",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(false)"
            }
          ],
          "expected": "Set<ValidationMessage> that is empty",
          "description": "A customizer that disables fail‑fast mode should not affect a valid instance; the validator runs to completion and returns an empty message set."
        }
      ]
    },
    {
      "group": "Consumer customizes ExecutionContext – fail‑fast mode",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "Set<ValidationMessage> containing exactly one message about missing required property \"name\"",
          "description": "When fail‑fast is enabled the validator should stop after the first error; the missing required property produces a single ValidationMessage."
        }
      ]
    },
    {
      "group": "Consumer throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failure\"",
          "description": "If the consumer throws an exception the method must propagate it unchanged; no validation should be performed."
        }
      ]
    },
    {
      "group": "Validation produces errors (multiple branches)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": -5 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set<ValidationMessage> containing at least two messages: one for missing required property \"name\" and one for violating the \"minimum\" constraint on \"age\"",
          "description": "A node that violates two independent schema rules triggers two separate branches inside the validator (required‑property check and numeric‑range check). Both messages must be present in the result set."
        }
      ]
    },
    {
      "group": "Schema with recursive anchor – branch exercised",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"self\": { \"$ref\": \"#\" } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "Set<ValidationMessage> that is empty (recursive anchor resolves without infinite loop)",
          "description": "When the schema declares a recursive anchor, the validator must take the `isRecursiveAnchor()` branch; the test verifies that recursion is handled correctly and no stack overflow occurs."
        }
      ]
    }
  ]
}