{
  "newTestCases": [
    {
      "group": "collectAnnotations - underlying false result",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true and getKeyword() = \"type\"; collectAnnotations(ExecutionContext, \"type\") stubbed to return false"
            }
          ],
          "expected": "false",
          "description": "When the ExecutionContext is valid and the validator provides a non‑null keyword, the wrapper should delegate to the overloaded collectAnnotations method and propagate its boolean result. The stubbed overload returns false, so the wrapper must also return false."
        }
      ]
    },
    {
      "group": "collectAnnotations - empty keyword handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true"
            }
          ],
          "expected": "false",
          "description": "If the validator’s getKeyword() returns an empty string (\"\"), the wrapper should treat it as a missing keyword and return false without delegating to the overload."
        }
      ]
    },
    {
      "group": "collectAnnotations - executionContext with immutable annotations map",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true; its annotations map is immutable (Collections.emptyMap())"
            }
          ],
          "expected": "false",
          "description": "Even though annotation collection is enabled, the underlying collectAnnotations(ExecutionContext, String) cannot add a new annotation because the map is immutable. The overload therefore returns false, and the wrapper must return false."
        }
      ]
    },
    {
      "group": "collectAnnotations - executionContext throws checked exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where isCollectAnnotationsEnabled() throws IllegalStateException"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the ExecutionContext throws a checked (runtime) exception while the wrapper checks the collection flag, the exception should propagate unchanged; the method must not swallow it."
        }
      ]
    }
  ]
}