{
  "testCases": [
    {
      "group": "Lazy loading when validators is null",
      "cases": [
        {
          "input": [],
          "expected": "The TypeValidator instance created inside getValidators() is returned; getValidators() is invoked exactly once.",
          "description": "The JsonSchema instance starts with `validators == null`. Calling getTypeValidator() should trigger lazy loading by invoking getValidators(), which populates both `validators` and `typeValidator`. The method must then return the newly created TypeValidator. The test verifies that getValidators() was called once (e.g., via a spy or overridden method) and that the returned object equals the TypeValidator set by getValidators()."
        }
      ]
    },
    {
      "group": "No lazy loading when validators already initialized",
      "cases": [
        {
          "input": [],
          "expected": "The pre‑set TypeValidator instance is returned; getValidators() is NOT invoked.",
          "description": "The JsonSchema instance is prepared with a non‑null `validators` list and a known `typeValidator` (using reflection or a test subclass). Calling getTypeValidator() must return the existing TypeValidator without calling getValidators(). The test asserts that the returned instance is the same as the one injected and that getValidators() was never invoked."
        }
      ]
    }
  ]
}