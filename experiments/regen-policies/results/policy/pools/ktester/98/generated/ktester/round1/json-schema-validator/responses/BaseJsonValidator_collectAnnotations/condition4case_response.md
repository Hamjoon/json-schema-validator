{
  "testCases": [
    {
      "group": "collectAnnotations - basic delegation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true"
            }
          ],
          "expected": "true",
          "description": "When a non‑null ExecutionContext is provided and the validator’s keyword is non‑null, the method should delegate to collectAnnotations(ExecutionContext, String) and return the boolean result of that call (expected true for the happy‑path mock)."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            }
          ],
          "expected": "false",
          "description": "If the supplied ExecutionContext is null the method should guard against NPE and return false without delegating."
        }
      ]
    },
    {
      "group": "collectAnnotations - keyword handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true"
            }
          ],
          "expected": "true",
          "description": "Validator returns a non‑null keyword (e.g., \"type\"). The overload should treat it as a normal keyword and return true."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true"
            }
          ],
          "expected": "false",
          "description": "Validator’s getKeyword() returns null. The method should detect the missing keyword and return false."
        }
      ]
    },
    {
      "group": "collectAnnotations - annotation collection disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = false"
            }
          ],
          "expected": "false",
          "description": "When the ExecutionContext signals that annotation collection is disabled, the method must short‑circuit and return false regardless of the keyword."
        }
      ]
    },
    {
      "group": "collectAnnotations - exception handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext that throws RuntimeException when getAnnotations() is called"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the underlying collectAnnotations(ExecutionContext, String) throws an exception, the wrapper method should propagate it (no swallow)."
        }
      ]
    }
  ]
}