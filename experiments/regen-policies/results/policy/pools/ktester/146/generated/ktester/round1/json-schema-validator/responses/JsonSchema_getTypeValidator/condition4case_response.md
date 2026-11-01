{
  "testCases": [
    {
      "group": "validators is null – lazy loading triggers getValidators()",
      "cases": [
        {
          "input": [],
          "expected": "typeValidator is the dummy instance set by overridden getValidators(); getValidatorsCalled flag is true",
          "description": "When the internal `validators` field is null, `getTypeValidator()` should invoke `getValidators()`. The overridden `getValidators()` in the test subclass records that it was called and initializes `typeValidator` with a dummy `TypeValidator`. The method then returns this dummy instance."
        }
      ]
    },
    {
      "group": "validators already initialized – getValidators() not called",
      "cases": [
        {
          "input": [],
          "expected": "typeValidator is the pre‑set dummy instance; getValidatorsCalled flag is false",
          "description": "When `validators` is already non‑null, `getTypeValidator()` must return the existing `typeValidator` without calling `getValidators()`. The test pre‑populates both `validators` and `typeValidator` fields and verifies that the method returns the same `typeValidator` and that the overridden `getValidators()` is never invoked."
        }
      ]
    }
  ]
}