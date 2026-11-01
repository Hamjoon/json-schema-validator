{
    "testCases": [
        {
            "group": "collectAnnotations(ExecutionContext, String) - execution config null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(getExecutionConfig() returns null)"
                        },
                        {
                            "parameter": "keyword",
                            "value": "\"anyKeyword\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the ExecutionContext returns a null ExecutionConfig, the method attempts to call isAnnotationCollectionEnabled() on a null reference and must throw NullPointerException."
                }
            ]
        },
        {
            "group": "collectAnnotations(ExecutionContext, String) - execution config method exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(getExecutionConfig().isAnnotationCollectionEnabled() throws IllegalStateException)"
                        },
                        {
                            "parameter": "keyword",
                            "value": "\"anyKeyword\""
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If the ExecutionConfig's isAnnotationCollectionEnabled() method throws an exception, the collectAnnotations method must propagate that exception."
                }
            ]
        },
        {
            "group": "collectAnnotations(ExecutionContext, String) - filter throws NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(annotationCollectionEnabled = true, filter = predicate that throws NullPointerException for null keyword)"
                        },
                        {
                            "parameter": "keyword",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the filter's test() method throws NullPointerException for a null keyword, the exception should be propagated by collectAnnotations."
                }
            ]
        },
        {
            "group": "collectAnnotations(ExecutionContext, String) - filter throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(annotationCollectionEnabled = true, filter = predicate that throws IllegalArgumentException for \"illegalKeyword\")"
                        },
                        {
                            "parameter": "keyword",
                            "value": "\"illegalKeyword\""
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the configured filter throws IllegalArgumentException for a specific keyword, collectAnnotations must let that exception bubble up."
                }
            ]
        }
    ]
}