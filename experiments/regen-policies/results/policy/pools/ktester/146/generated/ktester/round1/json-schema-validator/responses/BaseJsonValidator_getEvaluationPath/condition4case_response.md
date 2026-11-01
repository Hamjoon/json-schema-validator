{
    "group": "BaseJsonValidator.getEvaluationPath() - basic behavior",
    "cases": [
        {
            "input": [],
            "expected": "Returns the same JsonNodePath instance that was set during construction (non‑null path).",
            "description": "A concrete subclass of BaseJsonValidator is created with a predefined non‑null JsonNodePath. The test invokes getEvaluationPath() and verifies that the returned object is exactly the same instance that was supplied, confirming the getter does not create a new object or alter the reference."
        },
        {
            "input": [],
            "expected": "Returns null when the internal evaluationPath field has not been initialized.",
            "description": "A concrete subclass of BaseJsonValidator is instantiated without assigning a value to the protected evaluationPath field (leaving it null). Calling getEvaluationPath() should return null, ensuring the method correctly propagates the field's default state."
        }
    ]
}