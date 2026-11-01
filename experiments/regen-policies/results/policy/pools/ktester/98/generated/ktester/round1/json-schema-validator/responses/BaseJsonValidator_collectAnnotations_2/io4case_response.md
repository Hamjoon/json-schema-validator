{
  "testCases": [
    {
      "group": "collectAnnotations(ExecutionContext, String) - null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "keyword",
              "value": "\"anyKeyword\""
            }
          ],
          "expected": "NullPointerException",
          "description": "When the ExecutionContext argument is null the method dereferences it and must throw a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { executionConfig = null }"
            },
            {
              "parameter": "keyword",
              "value": "\"anyKeyword\""
            }
          ],
          "expected": "NullPointerException",
          "description": "If the ExecutionContext exists but its ExecutionConfig is null, the call to getExecutionConfig() returns null and the subsequent dereference throws a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = null }"
            },
            {
              "parameter": "keyword",
              "value": "\"anyKeyword\""
            }
          ],
          "expected": "NullPointerException",
          "description": "When the filter predicate inside ExecutionConfig is null, invoking test(keyword) results in a NullPointerException."
        }
      ]
    },
    {
      "group": "collectAnnotations(ExecutionContext, String) - keyword edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = k -> k == null }"
            },
            {
              "parameter": "keyword",
              "value": "null"
            }
          ],
          "expected": "true",
          "description": "Filter explicitly accepts a null keyword; with annotation collection enabled the method should return true."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = Objects::nonNull }"
            },
            {
              "parameter": "keyword",
              "value": "null"
            }
          ],
          "expected": "false",
          "description": "Filter rejects null keywords (Objects::nonNull); method must return false even though collection is enabled."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = s -> s.isEmpty() }"
            },
            {
              "parameter": "keyword",
              "value": "\"\""
            }
          ],
          "expected": "true",
          "description": "Empty string keyword satisfies a filter that checks for emptiness; method should return true."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = s -> !s.isEmpty() }"
            },
            {
              "parameter": "keyword",
              "value": "\"\""
            }
          ],
          "expected": "false",
          "description": "Empty string keyword does not satisfy a filter that requires non‑empty strings; method must return false."
        }
      ]
    },
    {
      "group": "collectAnnotations(ExecutionContext, String) - specific filter logic",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = k -> \"allowed\".equals(k) }"
            },
            {
              "parameter": "keyword",
              "value": "\"allowed\""
            }
          ],
          "expected": "true",
          "description": "Filter accepts only the literal keyword \"allowed\"; providing that keyword should yield true."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = k -> \"allowed\".equals(k) }"
            },
            {
              "parameter": "keyword",
              "value": "\"different\""
            }
          ],
          "expected": "false",
          "description": "Filter accepts only \"allowed\"; a different keyword must be rejected, resulting in false."
        }
      ]
    }
  ]
}