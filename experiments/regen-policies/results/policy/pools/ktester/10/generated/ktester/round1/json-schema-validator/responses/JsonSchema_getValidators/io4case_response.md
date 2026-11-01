{
  "testCases": [
    {
      "group": "Concurrent lazy initialization",
      "cases": [
        {
          "input": [],
          "expected": "read() is invoked exactly once even when getValidators() is called concurrently from multiple threads; both threads receive the same unmodifiable list instance (reference equality). No UnsupportedOperationException is thrown during normal access.",
          "description": "Create a Mockito spy of JsonSchema, stub read() to return a mutable list [validatorX, validatorY] and stub getSchemaNode() appropriately. Start two threads that each call getValidators() after a CountDownLatch releases them simultaneously. After both threads finish, verify that read() was called only once and that the two returned list references are identical (==). Also assert that the returned list is unmodifiable (add throws UnsupportedOperationException)."
        }
      ]
    },
    {
      "group": "Original list mutation does not affect cached validators",
      "cases": [
        {
          "input": [],
          "expected": "The list returned by getValidators() remains unchanged after the original mutable list (returned by read()) is modified post‑initialisation.",
          "description": "Spy on JsonSchema and stub read() to return a mutable ArrayList containing validatorA. Call getValidators() to trigger lazy initialisation. Afterwards, add validatorB to the original mutable list. Verify that the list returned by getValidators() still contains only validatorA and that its size is unchanged, confirming a defensive copy was made before wrapping with Collections.unmodifiableList."
        }
      ]
    },
    {
      "group": "Pre‑populated validators field bypasses read()",
      "cases": [
        {
          "input": [],
          "expected": "getValidators() returns the pre‑set list without invoking read(); the returned list is the exact instance set via reflection and is wrapped as unmodifiable.",
          "description": "Using reflection, set the private 'validators' field of a real JsonSchema instance to an unmodifiable list created from [validatorC]. Call getValidators() and verify that read() is never called (Mockito verify with never()) and that the returned list reference equals the one injected via reflection."
        }
      ]
    },
    {
      "group": "Preservation of order and content from read()",
      "cases": [
        {
          "input": [],
          "expected": "The list returned by getValidators() contains validators in the same order as produced by read().",
          "description": "Stub read() to return a mutable list [validator1, validator2, validator3]. After invoking getValidators(), assert that the returned list size is 3 and that iterator traversal yields validator1, then validator2, then validator3, confirming order preservation."
        }
      ]
    },
    {
      "group": "Read returns already unmodifiable list",
      "cases": [
        {
          "input": [],
          "expected": "getValidators() returns the same unmodifiable list instance returned by read() (no additional wrapping needed) and does not throw an exception.",
          "description": "Stub read() to return Collections.unmodifiableList(Arrays.asList(validatorU)). Call getValidators() and verify that the returned list is the exact same instance (==) as the one returned by read(), confirming that wrapping an already unmodifiable list does not cause issues."
        }
      ]
    }
  ]
}