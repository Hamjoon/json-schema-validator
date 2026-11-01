{
    "group": "collectAnnotations(ExecutionContext, String) - filter predicate exception",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = k -> { throw new IllegalStateException(\"filter failure\"); } }"
                },
                {
                    "parameter": "keyword",
                    "value": "\"anyKeyword\""
                }
            ],
            "expected": "IllegalStateException",
            "description": "The filter predicate throws an IllegalStateException; the method should propagate this exception."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext { annotationCollectionEnabled = true, annotationCollectionFilter = k -> { if (\"bad\".equals(k)) throw new RuntimeException(\"bad keyword\"); return true; } }"
                },
                {
                    "parameter": "keyword",
                    "value": "\"bad\""
                }
            ],
            "expected": "RuntimeException",
            "description": "The filter predicate throws a RuntimeException for a specific keyword; the method must propagate the exception."
        }
    ]
}