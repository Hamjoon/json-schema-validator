{
  "testCases": [
    {
      "group": "collectAnnotations - getKeyword throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true"
            }
          ],
          "expected": "RuntimeException",
          "description": "A subclass of BaseJsonValidator overrides getKeyword() to throw a RuntimeException. The wrapper should not catch it and must let the exception propagate."
        }
      ]
    },
    {
      "group": "collectAnnotations - isCollectAnnotationsEnabled throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where isCollectAnnotationsEnabled() throws NullPointerException"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the ExecutionContext throws a NullPointerException while the wrapper checks the collection flag, the exception must propagate unchanged."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext where isCollectAnnotationsEnabled() throws IllegalArgumentException"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If isCollectAnnotationsEnabled() throws an IllegalArgumentException, the wrapper should not swallow it and should propagate the same exception."
        }
      ]
    },
    {
      "group": "collectAnnotations - underlying overload throws Error",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mocked ExecutionContext with collectAnnotationsEnabled = true; overload collectAnnotations(ExecutionContext, String) throws AssertionError"
            }
          ],
          "expected": "AssertionError",
          "description": "The overloaded collectAnnotations method throws an AssertionError (a subclass of java.lang.Error). The wrapper must let the Error propagate without conversion."
        }
      ]
    }
  ]
}