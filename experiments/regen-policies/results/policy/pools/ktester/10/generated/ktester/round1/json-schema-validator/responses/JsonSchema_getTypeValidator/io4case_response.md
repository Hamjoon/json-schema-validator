{
  "testCases": [
    {
      "group": "Repeated lazy loading calls",
      "cases": [
        {
          "input": [],
          "expected": "First call triggers getValidators(); subsequent calls return the same non‑null TypeValidator instance without invoking getValidators() again",
          "description": "Create a JsonSchema spy where getValidators() is stubbed to populate validators and typeValidator. Call getTypeValidator() twice. Verify getValidators() was called exactly once and both returned references are identical."
        }
      ]
    },
    {
      "group": "Pre‑initialized validators without TypeValidator",
      "cases": [
        {
          "input": [],
          "expected": "null is returned; getValidators() is NOT invoked",
          "description": "Manually set the `validators` field to an empty ArrayList and leave `typeValidator` as null. Invoke getTypeValidator() and assert that the result is null and that getValidators() was never called."
        }
      ]
    },
    {
      "group": "Validators list empty after lazy loading",
      "cases": [
        {
          "input": [],
          "expected": "null is returned; getValidators() is invoked exactly once",
          "description": "Mock getValidators() to set `validators` to an empty list and leave `typeValidator` null. Call getTypeValidator() and verify that it returns null while confirming that getValidators() was executed once."
        }
      ]
    },
    {
      "group": "Exception propagation from getValidators",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException thrown from getValidators() propagates out of getTypeValidator()",
          "description": "Configure the JsonSchema spy so that getValidators() throws a RuntimeException (e.g., new IllegalStateException(\"boom\")). Invoke getTypeValidator() and assert that the same exception is thrown to the caller."
        }
      ]
    },
    {
      "group": "Custom TypeValidator subclass returned",
      "cases": [
        {
          "input": [],
          "expected": "Instance of the custom subclass is returned; getValidators() invoked once",
          "description": "Create a subclass of TypeValidator (e.g., MySpecialTypeValidator). Stub getValidators() to assign this instance to `typeValidator`. After calling getTypeValidator(), verify that the returned object is an instance of MySpecialTypeValidator and that getValidators() was called exactly once."
        }
      ]
    }
  ]
}