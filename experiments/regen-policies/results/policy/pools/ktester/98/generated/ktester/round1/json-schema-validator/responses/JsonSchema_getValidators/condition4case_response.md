{
  "testCases": [
    {
      "group": "getValidators - when validators is null",
      "cases": [
        {
          "input": [],
          "expected": "Returns an unmodifiable list containing the validators produced by read(schemaNode). The returned list cannot be modified (throws UnsupportedOperationException on add).",
          "description": "First call to getValidators() on a newly created JsonSchema instance where the internal `validators` field is null. The method should invoke `read(getSchemaNode())`, wrap the result with `Collections.unmodifiableList`, assign it to the field, and return it."
        }
      ]
    },
    {
      "group": "getValidators - when validators is already initialized",
      "cases": [
        {
          "input": [],
          "expected": "Returns the previously stored unmodifiable list without invoking read(). The same list instance is returned on subsequent calls.",
          "description": "Second (or later) call to getValidators() after the `validators` field has already been set. The method should bypass the `if` block and directly return the cached list, ensuring no additional call to `read()` occurs."
        }
      ]
    }
  ]
}