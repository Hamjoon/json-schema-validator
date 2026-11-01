{
  "testCases": [
    {
      "group": "Validators Initialization - first call (null branch)",
      "cases": [
        {
          "input": [],
          "expected": {
            "returnValue": "unmodifiable list containing the validators returned by read()",
            "behavior": [
              "read(JsonNode) is invoked exactly once",
              "the returned list cannot be modified (throws UnsupportedOperationException on add/remove)",
              "the list contains the mocked JsonValidator instances in the same order as the mocked read() result"
            ]
          },
          "description": "When a newly created JsonSchema has its internal 'validators' field set to null, invoking getValidators() should call the private read() method, wrap its result with Collections.unmodifiableList, assign it to the field, and return the unmodifiable list."
        }
      ]
    },
    {
      "group": "Validators Caching - subsequent call (non‑null branch)",
      "cases": [
        {
          "input": [],
          "expected": {
            "returnValue": "the same unmodifiable list instance that was created on the first call",
            "behavior": [
              "read(JsonNode) is NOT invoked on the second call",
              "the list reference returned is identical (==) to the one returned by the first call"
            ]
          },
          "description": "After the first call to getValidators() has populated the 'validators' field, a second call should bypass the null‑check, skip the read() invocation, and simply return the already cached unmodifiable list."
        }
      ]
    }
  ]
}