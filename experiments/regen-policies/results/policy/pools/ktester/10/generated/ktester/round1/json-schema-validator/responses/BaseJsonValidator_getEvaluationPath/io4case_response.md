{
    "group": "BaseJsonValidator.getEvaluationPath",
    "cases": [
        {
            "input": [],
            "expected": "JsonNodePath instance with path '/' (root only)",
            "description": "When the validator is constructed with an evaluationPath representing the root (e.g., new JsonNodePath(\"/\")), getEvaluationPath() should return that exact instance, confirming handling of the minimal non‑null path."
        },
        {
            "input": [],
            "expected": "JsonNodePath instance with path '/root/0/child[2]'",
            "description": "When the validator's evaluationPath includes array indices and nested properties (e.g., '/root/0/child[2]'), getEvaluationPath() must return the same complex path object, verifying that the getter works for any valid JSON pointer syntax."
        },
        {
            "input": [],
            "expected": "same object reference on successive calls",
            "description": "Calling getEvaluationPath() multiple times in a row should always return the identical object reference (reference equality), ensuring the method does not create new objects on each invocation."
        },
        {
            "input": [],
            "expected": "JsonNodePath instance with path '/new/path' after reassignment",
            "description": "If a subclass exposes a method to change the internal evaluationPath after construction (e.g., setEvaluationPath(new JsonNodePath(\"/new/path\"))), getEvaluationPath() must reflect the updated path, confirming that the getter returns the current state, not a cached original value."
        },
        {
            "input": [],
            "expected": "null after explicit reset to null",
            "description": "When a subclass explicitly resets the internal evaluationPath to null (e.g., setEvaluationPath(null)), getEvaluationPath() should return null, demonstrating that the getter reflects a deliberate nullification rather than retaining a previous non‑null value."
        }
    ]
}