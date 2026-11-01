{
  "newTestCases": [
    {
      "group": "getValidationContext - consistent across multiple calls",
      "cases": [
        {
          "input": [],
          "expected": "the same ValidationContext instance on every call (reference equality)",
          "description": "Create a JsonSchema via the JsonSchema.from(...) factory with a known ValidationContext. Call getValidationContext() several times in a row and assert that each returned reference is identical to the original instance."
        }
      ]
    },
    {
      "group": "getValidationContext - inherited schema via fromRef shares the same context",
      "cases": [
        {
          "input": [],
          "expected": "the same ValidationContext instance as the parent schema (reference equality)",
          "description": "Build a parent JsonSchema with a specific ValidationContext, then obtain a child schema using parentSchema.fromRef(...). Verify that childSchema.getValidationContext() returns the exact same ValidationContext object that was supplied to the parent."
        }
      ]
    },
    {
      "group": "getValidationContext - unchanged after withConfig call",
      "cases": [
        {
          "input": [],
          "expected": "the original ValidationContext instance (reference equality)",
          "description": "Create a JsonSchema with a ValidationContext, invoke schema.withConfig(customConfig) to obtain a new JsonSchema instance, and confirm that the new instance's getValidationContext() still returns the original ValidationContext object."
        }
      ]
    },
    {
      "group": "getValidationContext - remains the same after validation execution",
      "cases": [
        {
          "input": [],
          "expected": "the same ValidationContext instance as before validation (reference equality)",
          "description": "Construct a JsonSchema with a known ValidationContext and a simple valid JSON schema. Call schema.validate(someJsonNode) to trigger validation logic, then call getValidationContext() again and verify that the returned object is still the original ValidationContext."
        }
      ]
    },
    {
      "group": "getValidationContext - thread‑safety of concurrent reads",
      "cases": [
        {
          "input": [],
          "expected": "identical ValidationContext instance returned by all concurrent threads (reference equality)",
          "description": "Create a JsonSchema with a ValidationContext. Start multiple threads (e.g., 10) that concurrently invoke getValidationContext() and store the returned reference. After all threads complete, assert that every stored reference points to the same ValidationContext object, confirming that the getter is safe for concurrent access."
        }
      ]
    }
  ]
}