{
  "testCases": [
    {
      "group": "BaseJsonValidator.getEvaluationPath - identity consistency",
      "cases": [
        {
          "input": [],
          "expected": "same JsonNodePath instance on repeated calls (reference equality)",
          "description": "Create a concrete subclass of BaseJsonValidator that sets a non‑null JsonNodePath via its protected constructor. Call getEvaluationPath() twice and assert that the returned references are identical (==). This verifies that the method does not create a new object on each invocation."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getEvaluationPath - distinct instances",
      "cases": [
        {
          "input": [],
          "expected": "different JsonNodePath objects for different validator instances",
          "description": "Instantiate two separate validator objects, each initialized with its own JsonNodePath instance. Verify that getEvaluationPath() on the first validator returns a different object than getEvaluationPath() on the second validator. This ensures the field is instance‑specific and not shared statically."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getEvaluationPath - complex path content",
      "cases": [
        {
          "input": [],
          "expected": "JsonNodePath whose string representation matches the expected JSON Pointer (e.g., \"#/properties/address/properties/street\")",
          "description": "Create a validator with a JsonNodePath representing a nested schema location (e.g., new JsonNodePath(\"#/properties/address/properties/street\")). Call getEvaluationPath() and assert that the returned object's toString() (or its internal path list) equals the expected pointer string. This checks that the method returns the exact path object that was supplied during construction."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getEvaluationPath - mock path object",
      "cases": [
        {
          "input": [],
          "expected": "the exact mock JsonNodePath instance injected via constructor",
          "description": "Use a Mockito (or similar) mock of JsonNodePath that records method calls. Inject this mock into a concrete validator subclass. Invoke getEvaluationPath() and verify that the returned reference is the same mock instance (using ==) and that no additional interactions with the mock occurred. This confirms that the getter simply returns the field without side effects."
        }
      ]
    }
  ]
}