{
    "group": "collectAnnotations(ExecutionContext, String) - boolean decision branches",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = false, filter = any)"
                },
                {
                    "parameter": "keyword",
                    "value": "\"anyKeyword\""
                }
            ],
            "expected": false,
            "description": "Annotation collection is globally disabled; method must return false regardless of the filter result."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = true, filter = predicate returns false for \"keywordA\")"
                },
                {
                    "parameter": "keyword",
                    "value": "\"keywordA\""
                }
            ],
            "expected": false,
            "description": "Annotation collection is enabled but the configured filter rejects the supplied keyword; method must return false."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = true, filter = predicate returns true for \"keywordB\")"
                },
                {
                    "parameter": "keyword",
                    "value": "\"keywordB\""
                }
            ],
            "expected": true,
            "description": "Annotation collection is enabled and the filter accepts the keyword; method must return true."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = true, filter = predicate returns true for null keyword)"
                },
                {
                    "parameter": "keyword",
                    "value": "null"
                }
            ],
            "expected": true,
            "description": "Keyword is null but the filter is designed to accept null values; with collection enabled the method should still return true."
        }
    ]
}