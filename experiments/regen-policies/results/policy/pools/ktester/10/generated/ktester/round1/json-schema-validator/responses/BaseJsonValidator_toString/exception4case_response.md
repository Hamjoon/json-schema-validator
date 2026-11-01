{
    "test_cases": [
        {
            "group": "BaseJsonValidator.toString – getEvaluationPath throws IllegalStateException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "A subclass of BaseJsonValidator overrides getEvaluationPath() to throw IllegalStateException. The toString() method should propagate this unchecked exception unchanged."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – getEvaluationPath throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.RuntimeException",
                    "description": "A subclass of BaseJsonValidator overrides getEvaluationPath() to throw a generic RuntimeException. The test verifies that toString() does not catch the exception and it propagates."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – getName throws UnsupportedOperationException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.UnsupportedOperationException",
                    "description": "A custom JsonNodePath implementation is returned by getEvaluationPath() where getName(-1) deliberately throws UnsupportedOperationException. The toString() method should propagate this exception."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – getName throws IllegalArgumentException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalArgumentException",
                    "description": "A custom JsonNodePath implementation is returned by getEvaluationPath() where getName(-1) validates the index and throws IllegalArgumentException for negative values. The test ensures toString() propagates the exception."
                }
            ]
        }
    ]
}