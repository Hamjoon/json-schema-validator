{
  "test_cases": [
    {
      "group": "preloadJsonSchema - null internal schemas list",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException is thrown when `schemas` field is null.",
          "description": "Using reflection, set the private `schemas` field of an `AnyOfValidator` instance to null. Invoking `preloadJsonSchema()` should result in a NullPointerException because the method attempts to pass a null list to `preloadJsonSchemas`."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - unmodifiable schemas list",
      "cases": [
        {
          "input": [],
          "expected": "UnsupportedOperationException is thrown (or propagated) when `preloadJsonSchemas` attempts to modify the unmodifiable list.",
          "description": "Replace the private `schemas` list with an unmodifiable view (e.g., `Collections.unmodifiableList(new ArrayList<>())`) via reflection. If `preloadJsonSchemas` tries to add or remove elements, an UnsupportedOperationException should be thrown and propagated out of `preloadJsonSchema()`."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - error propagation",
      "cases": [
        {
          "input": [],
          "expected": "AssertionError (or any subclass of java.lang.Error) thrown from overridden `preloadJsonSchemas` propagates unchanged.",
          "description": "Create a subclass or spy of `AnyOfValidator` where `preloadJsonSchemas` is overridden to throw an `AssertionError`. Calling `preloadJsonSchema()` must let this error bubble up, confirming that the method does not catch or wrap Errors."
        }
      ]
    }
  ]
}