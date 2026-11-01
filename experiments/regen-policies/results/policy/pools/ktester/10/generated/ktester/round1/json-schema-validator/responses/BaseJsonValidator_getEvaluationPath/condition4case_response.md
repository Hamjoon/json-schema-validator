{
    "group": "BaseJsonValidator.getEvaluationPath",
    "cases": [
        {
            "input": [],
            "expected": "JsonNodePath instance with path '/root/child'",
            "description": "When the validator's internal evaluationPath is set to a non‑null JsonNodePath (e.g., '/root/child'), getEvaluationPath() should return the same instance, confirming that the getter simply returns the stored value."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the validator's internal evaluationPath is left null (no path has been assigned), getEvaluationPath() should return null, verifying that the method does not create a default value or throw an exception."
        },
        {
            "input": [],
            "expected": "same object reference as the one assigned during construction",
            "description": "Ensures that the returned object is the exact same reference that was injected into the validator (reference equality), not a copy or a new instance."
        }
    ]
}