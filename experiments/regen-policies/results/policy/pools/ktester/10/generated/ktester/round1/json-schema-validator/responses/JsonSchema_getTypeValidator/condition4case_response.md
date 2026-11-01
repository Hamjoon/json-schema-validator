{
  "testCases": [
    {
      "group": "Lazy loading behavior",
      "cases": [
        {
          "input": [],
          "expected": "Non‑null TypeValidator instance returned; getValidators() method is invoked exactly once",
          "description": "When the internal `validators` field is null, calling `getTypeValidator()` should trigger lazy loading via `getValidators()`. The mocked `getValidators()` populates both `validators` and `typeValidator`. The test verifies that the returned object is not null and that `getValidators()` was called."
        }
      ]
    },
    {
      "group": "Already initialized behavior",
      "cases": [
        {
          "input": [],
          "expected": "The previously set TypeValidator instance is returned; getValidators() is NOT invoked",
          "description": "The `validators` list is pre‑populated (e.g., with a dummy validator) and `typeValidator` is set to a known stub. Calling `getTypeValidator()` must return that stub without calling `getValidators()`. The test uses a spy or Mockito `verify` to ensure `getValidators()` is never invoked."
        }
      ]
    },
    {
      "group": "Null TypeValidator after loading",
      "cases": [
        {
          "input": [],
          "expected": "null is returned; getValidators() is invoked but does not set typeValidator",
          "description": "Simulate a schema that does not contain a `type` keyword. The mocked `getValidators()` populates `validators` with other validators but leaves `typeValidator` as null. After invoking `getTypeValidator()`, the method should return null, confirming that the lazy‑load path works even when no TypeValidator exists."
        }
      ]
    }
  ]
}