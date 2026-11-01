{
  "testCases": [
    {
      "group": "Successful validation (default behavior)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors)",
          "description": "Validate a JSON document that fully conforms to the schema when no ExecutionContextCustomizer is supplied. The method should create a default ExecutionContext, run all validators and return an empty set."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that does nothing (e -> {})"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors)",
          "description": "Validate the same valid JSON but with a no‑op customizer. The customizer must not alter the context, so the result should be identical to the previous case."
        }
      ]
    },
    {
      "group": "Validation failures",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "A non‑empty Set<ValidationMessage> containing at least two messages: one for type mismatch on 'name' and one for type mismatch on 'age'",
          "description": "The JSON does not satisfy the schema (wrong types). The method should collect validation messages for each failing validator."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that disables the 'required' validator"
            }
          ],
          "expected": "An empty Set<ValidationMessage> because the customizer removed the required‑field check",
          "description": "A customizer that mutates the ExecutionContext to remove the RequiredValidator should allow an otherwise invalid (missing required properties) JSON to pass."
        }
      ]
    },
    {
      "group": "Edge cases and error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that rootNode must not be null",
          "description": "Passing a null JsonNode should be rejected early, typically with an NPE or a dedicated argument‑validation exception."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that throws RuntimeException"
            }
          ],
          "expected": "RuntimeException propagates out of validate method",
          "description": "If the customizer itself throws an exception, validate should not swallow it; the exception should bubble up."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ExecutionContextCustomizer that sets ExecutionContext.isRecursiveAnchor=true"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (validation completes without infinite recursion)",
          "description": "When the schema is marked as a recursive anchor, the method must handle it specially to avoid endless loops. The test verifies that validation terminates and returns the correct result."
        }
      ]
    }
  ]
}