{
  "group": "BaseJsonValidator.getEvaluationPath",
  "cases": [
    {
      "input": [],
      "expected": "JsonNodePath instance that was set during construction (non‑null)",
      "description": "Verify that getEvaluationPath() returns the same JsonNodePath object that the validator was initialized with. A concrete subclass of BaseJsonValidator is created with a mock JsonNodePath injected via a protected constructor; the test asserts the returned reference equals the injected instance."
    },
    {
      "input": [],
      "expected": "null",
      "description": "Verify that getEvaluationPath() correctly returns null when the internal evaluationPath field has not been initialized. A concrete subclass is instantiated without setting evaluationPath (using reflection to ensure the field stays null), and the test asserts that the method returns null."
    }
  ]
}