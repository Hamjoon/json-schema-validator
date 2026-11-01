{
  "testCases": [
    {
      "group": "Null ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            }
          ],
          "expected": "false",
          "description": "When the supplied ExecutionContext is null the method should safely handle it and return false without throwing an exception."
        }
      ]
    },
    {
      "group": "Annotations collection disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContextMock { collectAnnotations = false }"
            }
          ],
          "expected": "false",
          "description": "If the ExecutionContext indicates that annotation collection is disabled (e.g., `isCollectAnnotations()` returns false), the method must skip any annotation logic and return false."
        }
      ]
    },
    {
      "group": "Keyword is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContextMock { collectAnnotations = true }"
            }
          ],
          "expected": "false",
          "description": "When the validator’s keyword (returned by `getKeyword()`) is null the method cannot decide which annotation to store, therefore it should return false."
        }
      ]
    },
    {
      "group": "Known keyword – annotation stored successfully",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContextMock { collectAnnotations = true, annotations = [] }"
            }
          ],
          "expected": "true",
          "description": "For a non‑null ExecutionContext with collection enabled and a known keyword (e.g., \"type\"), the validator should create a JsonNodeAnnotation, add it to the context and return true."
        }
      ]
    },
    {
      "group": "Unknown keyword – no annotation stored",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContextMock { collectAnnotations = true, annotations = [] }"
            }
          ],
          "expected": "false",
          "description": "When the keyword is not recognised by the internal switch/case (e.g., \"unknownKeyword\"), the method must skip annotation creation and return false."
        }
      ]
    },
    {
      "group": "Exception during annotation customization",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ExecutionContextMock { collectAnnotations = true, customizerThrows = true }"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the consumer passed to `putAnnotation` throws an exception, the method should propagate it (or wrap it) rather than swallowing it, allowing the test to verify exception handling."
        }
      ]
    }
  ]
}