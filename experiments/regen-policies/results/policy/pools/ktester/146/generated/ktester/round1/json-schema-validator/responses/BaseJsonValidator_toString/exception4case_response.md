{
    "group": "BaseJsonValidator.toString() - getEvaluationPath() exception propagation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "evaluationPathThrows",
                    "value": "java.lang.IllegalStateException"
                }
            ],
            "expected": "java.lang.IllegalStateException",
            "description": "When getEvaluationPath() throws an IllegalStateException, toString() must not catch it and the same exception should propagate to the caller."
        },
        {
            "input": [
                {
                    "parameter": "evaluationPathThrows",
                    "value": "java.lang.RuntimeException"
                }
            ],
            "expected": "java.lang.RuntimeException",
            "description": "When getEvaluationPath() throws a generic RuntimeException, toString() should allow the exception to bubble up unchanged."
        }
    ]
}