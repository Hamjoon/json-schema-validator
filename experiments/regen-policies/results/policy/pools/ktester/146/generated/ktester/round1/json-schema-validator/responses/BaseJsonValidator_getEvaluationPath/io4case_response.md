{
  "tests": [
    {
      "group": "BaseJsonValidator.getEvaluationPath() - reference identity after path reassignment",
      "cases": [
        {
          "input": [],
          "expected": "Returns the newly assigned JsonNodePath instance (same object identity) after the internal evaluationPath field is changed post‑construction.",
          "description": "A concrete subclass of BaseJsonValidator is instantiated with an initial JsonNodePath (e.g., \"/properties/name\"). Using reflection (or a protected setter method) the evaluationPath field is reassigned to a different JsonNodePath (e.g., \"/properties/age\"). The test then calls getEvaluationPath() and asserts that the returned object is exactly the same instance that was assigned (identity equality), confirming that the getter reflects runtime changes to the field."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getEvaluationPath() - consistency across multiple calls",
      "cases": [
        {
          "input": [],
          "expected": "Subsequent calls return the identical JsonNodePath instance (object identity) each time.",
          "description": "A concrete validator is created with a non‑null JsonNodePath (e.g., \"/items/0\"). The test invokes getEvaluationPath() twice (or more) and verifies that both returned references are the exact same object (using ==). This ensures the getter does not create new objects on each call."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getEvaluationPath() - independent instances maintain separate paths",
      "cases": [
        {
          "input": [],
          "expected": "Each validator instance returns its own JsonNodePath; the paths are not shared between instances.",
          "description": "Two separate concrete subclasses of BaseJsonValidator are constructed, each with a distinct JsonNodePath (e.g., \"/a\" for the first, \"/b\" for the second). The test calls getEvaluationPath() on both validators and asserts that the returned objects are not the same instance and that each matches the path supplied to its respective constructor. This verifies that evaluationPath is instance‑specific."
        }
      ]
    }
  ]
}