{
  "testCases": [
    {
      "group": "Malformed $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with schema containing $ref \"#/invalid%zz\""
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "IllegalArgumentException thrown from URLDecoder.decode indicating malformed percent‑encoding in $ref",
          "description": "The schema includes a $ref with an illegal percent‑encoding sequence. When validateAndCollect attempts to resolve the reference it should trigger URLDecoder.decode which throws IllegalArgumentException. The method must let this exception surface."
        }
      ]
    },
    {
      "group": "WalkListenerRunner unexpected exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with WalkListenerRunner that throws RuntimeException(\"listener failure\") on first node"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"a\":1}"
            }
          ],
          "expected": "RuntimeException with message \"listener failure\" propagates out of validateAndCollect",
          "description": "When a WalkListenerRunner attached to the schema throws an unexpected RuntimeException (not WalkAbortException), validateAndCollect should not swallow it; the exception must propagate to the caller."
        }
      ]
    },
    {
      "group": "Circular $ref without recursive anchor – recursion limit exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with schema that $ref‑s to itself and isRecursiveAnchor() returns false"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {}"
            }
          ],
          "expected": "ValidationException (or IllegalStateException) indicating recursion depth exceeded or circular reference detected",
          "description": "A schema that references itself via $ref but is not marked as a recursive anchor should cause the validator to detect a circular reference and abort with a specific exception rather than entering an infinite recursion."
        }
      ]
    },
    {
      "group": "Validator throws JsonProcessingException – wrapper exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with a custom JsonValidator that throws JsonProcessingException(\"invalid JSON\") during validation"
            },
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"field\":\"value\"}"
            }
          ],
          "expected": "ValidationException (or RuntimeException) whose cause is JsonProcessingException with message \"invalid JSON\"",
          "description": "If a JsonValidator invoked by validateAndCollect throws a JsonProcessingException, the method should wrap it in a higher‑level ValidationException (or rethrow as RuntimeException) so that callers can see the original cause."
        }
      ]
    }
  ]
}