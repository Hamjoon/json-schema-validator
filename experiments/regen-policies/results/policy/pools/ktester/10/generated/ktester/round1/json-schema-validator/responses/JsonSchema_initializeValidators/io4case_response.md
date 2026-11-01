{
  "testCases": [
    {
      "group": "initializeValidators - empty validators list",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchema() is not invoked for any validator and validatorsLoaded becomes true",
          "description": "When getValidators() returns an empty list and validatorsLoaded is false, the method should skip the loop, not call any preload, and set validatorsLoaded to true."
        }
      ]
    },
    {
      "group": "initializeValidators - partial failure during preload",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException is propagated; preloadJsonSchema() is invoked on the first validator, then on the second which throws, validatorsLoaded stays false and the third validator is never invoked",
          "description": "With multiple validators, if a validator in the middle throws an exception, the method must abort, propagate the exception, leave validatorsLoaded false, and ensure no further validators are processed."
        }
      ]
    },
    {
      "group": "initializeValidators - retry after previous failure",
      "cases": [
        {
          "input": [],
          "expected": "On first call, RuntimeException is thrown and validatorsLoaded remains false; on second call, all validators' preloadJsonSchema() are invoked and validatorsLoaded becomes true",
          "description": "Verifies that after a failed preload attempt the flag is not set, allowing a subsequent call to retry loading all validators successfully."
        }
      ]
    },
    {
      "group": "initializeValidators - concurrent first-time initialization",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchema() is invoked exactly once per validator even when two threads call initializeValidators() simultaneously; validatorsLoaded ends true",
          "description": "Simulates two concurrent invocations when validatorsLoaded is initially false to ensure the method behaves correctly (no double‑loading) and ends with validatorsLoaded true. (Implementation may rely on external synchronization; test asserts that only one set of preload calls occurs.)"
        }
      ]
    }
  ]
}