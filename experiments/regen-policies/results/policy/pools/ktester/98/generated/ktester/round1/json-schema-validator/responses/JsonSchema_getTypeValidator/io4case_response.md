{
  "testCases": [
    {
      "group": "Lazy loading invoked only once on repeated calls",
      "cases": [
        {
          "input": [],
          "expected": "The first call triggers getValidators() exactly once; subsequent calls return the same TypeValidator without invoking getValidators() again.",
          "description": "A fresh JsonSchema instance with `validators == null` is used. Call `getTypeValidator()` twice. Verify via a spy or overridden method that `getValidators()` is called only on the first invocation and that both returned objects are identical."
        }
      ]
    },
    {
      "group": "Return null when schema does not define a type validator",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Create a JsonSchema from a JSON schema that contains no `type` keyword (e.g., `{ \"properties\": { \"name\": { \"type\": \"string\" } } }`). After the schema is fully initialized, `getTypeValidator()` should return `null` because no TypeValidator was created during validator construction."
        }
      ]
    },
    {
      "group": "Existing validators list but null typeValidator returns null",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Manually inject a non‑null `validators` list (e.g., an empty `ArrayList`) into a JsonSchema instance via reflection while leaving the private `typeValidator` field as `null`. Calling `getTypeValidator()` must return `null` and must not trigger a lazy‑load call to `getValidators()`."
        }
      ]
    },
    {
      "group": "Custom subclass overrides getValidators without setting typeValidator",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Define a test subclass of JsonSchema that overrides `getValidators()` to return an empty list and deliberately does **not** assign a TypeValidator to the `typeValidator` field. With `validators` initially `null`, invoking `getTypeValidator()` should call the overridden `getValidators()` and then return `null` because no TypeValidator was created."
        }
      ]
    },
    {
      "group": "Concurrent access does not create multiple TypeValidator instances",
      "cases": [
        {
          "input": [],
          "expected": "All threads receive the same TypeValidator instance; `getValidators()` is invoked at most once.",
          "description": "Spawn several threads that simultaneously call `getTypeValidator()` on the same freshly created JsonSchema (where `validators == null`). After all threads complete, assert that the returned TypeValidator objects are identical (by reference) and that `getValidators()` was executed only once (e.g., using an atomic counter in an overridden subclass)."
        }
      ]
    }
  ]
}