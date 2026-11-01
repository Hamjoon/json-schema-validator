{
  "group": "collectAnnotations(ExecutionContext, String) - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext { annotationCollectionEnabled = false }"
        },
        {
          "parameter": "keyword",
          "value": "\"anyKeyword\""
        }
      ],
      "expected": "false",
      "description": "Annotation collection is globally disabled, therefore the method must return false regardless of the keyword or filter."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = keyword -> false }"
        },
        {
          "parameter": "keyword",
          "value": "\"unwantedKeyword\""
        }
      ],
      "expected": "false",
      "description": "Annotation collection is enabled but the filter predicate rejects the supplied keyword, so the method should return false."
    },
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = keyword -> true }"
        },
        {
          "parameter": "keyword",
          "value": "\"wantedKeyword\""
        }
      ],
      "expected": "true",
      "description": "Both annotation collection is enabled and the filter predicate accepts the keyword, therefore the method must return true."
    }
  ]
}