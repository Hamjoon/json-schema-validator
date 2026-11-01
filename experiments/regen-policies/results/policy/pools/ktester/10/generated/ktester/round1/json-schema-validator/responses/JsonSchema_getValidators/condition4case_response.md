{
  "testCases": [
    {
      "group": "Initial retrieval of validators (lazy initialization)",
      "cases": [
        {
          "input": [],
          "expected": "Returns an unmodifiable list containing the validators provided by the stubbed read() method. Attempting to modify the list throws UnsupportedOperationException. The private read() method is invoked exactly once.",
          "description": "When validators field is null, getValidators() should invoke read(getSchemaNode()), wrap the result with Collections.unmodifiableList, store it in the validators field, and return it. The test uses a Mockito spy on JsonSchema to stub read() to return a mutable list [validatorA, validatorB] and to stub getSchemaNode() to return a dummy JsonNode. After the first call, the returned list must be unmodifiable (add operation throws)."
        },
        {
          "input": [],
          "expected": "Returns the same unmodifiable list instance as the previous call. The private read() method is NOT invoked again.",
          "description": "After the lazy initialization has occurred, a second call to getValidators() must return the cached list without re‑executing read(). The test calls getValidators() twice on the same spied JsonSchema instance and verifies that read() was called only once and that the two returned references are identical (==)."
        }
      ]
    },
    {
      "group": "Handling of empty validator collection",
      "cases": [
        {
          "input": [],
          "expected": "Returns an unmodifiable empty list. No exception is thrown when calling size() or isEmpty().",
          "description": "If read() returns an empty mutable list, getValidators() should still wrap it in an unmodifiable list. The test stubs read() to return Collections.emptyList() and asserts that the returned list is empty and unmodifiable (add throws UnsupportedOperationException)."
        }
      ]
    },
    {
      "group": "Exception scenario when read() returns null",
      "cases": [
        {
          "input": [],
          "expected": "Throws NullPointerException because Collections.unmodifiableList(null) is invoked.",
          "description": "Although read() is expected to never return null, the method does not guard against it. The test forces read() to return null and verifies that getValidators() propagates a NullPointerException."
        }
      ]
    }
  ]
}