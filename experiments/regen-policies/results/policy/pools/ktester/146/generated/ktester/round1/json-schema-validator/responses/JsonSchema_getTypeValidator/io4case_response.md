{
  "testCases": [
    {
      "group": "validators null – lazy loading with getValidators not setting typeValidator",
      "cases": [
        {
          "input": [],
          "expected": "typeValidator remains null; getValidatorsCalled flag is true; validators list is initialized (non‑null)",
          "description": "When the internal `validators` field is null, `getTypeValidator()` should invoke the overridden `getValidators()`. The overridden method creates a non‑empty validators list but deliberately does **not** assign a `typeValidator`. The method must therefore return `null` while still having called `getValidators()` and populated `validators`."
        }
      ]
    },
    {
      "group": "validators already initialized but typeValidator null – no lazy loading",
      "cases": [
        {
          "input": [],
          "expected": "typeValidator is null; getValidatorsCalled flag is false",
          "description": "If `validators` is already non‑null (pre‑populated by the test) but `typeValidator` has not been set, `getTypeValidator()` must simply return `null` and must **not** invoke `getValidators()` again."
        }
      ]
    },
    {
      "group": "multiple getTypeValidator calls after lazy loading – getValidators called only once",
      "cases": [
        {
          "input": [],
          "expected": "first call returns dummy TypeValidator; second call returns same instance; getValidatorsCalled flag is true and remains true (no second invocation)",
          "description": "When `validators` is initially null, the first call to `getTypeValidator()` triggers lazy loading via `getValidators()` which sets a dummy `typeValidator`. A subsequent call should return the already‑initialized `typeValidator` without invoking `getValidators()` a second time. The test verifies that the flag indicating a `getValidators()` call stays true after both calls."
        }
      ]
    },
    {
      "group": "validators null – getValidators throws RuntimeException",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException propagated from getValidators; typeValidator remains null",
          "description": "If the overridden `getValidators()` throws a `RuntimeException` (simulating an unexpected error during lazy loading), `getTypeValidator()` must not swallow the exception; it should propagate it to the caller. The test confirms that the exception type and message match the one thrown by `getValidators()`."
        }
      ]
    }
  ]
}