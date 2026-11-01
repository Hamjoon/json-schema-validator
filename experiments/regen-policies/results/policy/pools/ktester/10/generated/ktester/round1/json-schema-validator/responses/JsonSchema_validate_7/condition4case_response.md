{
  "testCases": [
    {
      "group": "Valid validation with default customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\", \"age\": 30 }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat" 
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "A non‑null formatted result (e.g., an empty string when validation succeeds).",
          "description": "All arguments are non‑null and the JSON instance conforms to the schema; the customizer does nothing, so validation should succeed and the format should produce the expected output."
        }
      ]
    },
    {
      "group": "Valid validation with customizer that modifies context",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"id\": 123 }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> executionContext.setAttribute(\"skipRequired\", Boolean.TRUE)"
            }
          ],
          "expected": "A formatted result that reflects the altered validation behavior (e.g., required‑field errors are suppressed).",
          "description": "The customizer sets an attribute in the ExecutionContext that the validator checks to skip the required‑field validator, exercising the branch where the context influences validation."
        }
      ]
    },
    {
      "group": "Null rootNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that rootNode must not be null.",
          "description": "Passing a null JsonNode should trigger the null‑check branch and result in an exception."
        }
      ]
    },
    {
      "group": "Null format",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"valid\": true }"
            },
            {
              "parameter": "format",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that format must not be null.",
          "description": "A null OutputFormat triggers the branch that validates the format argument."
        }
      ]
    },
    {
      "group": "Null executionCustomizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"field\": \"value\" }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "A formatted result identical to the case where a no‑op customizer is supplied.",
          "description": "When the consumer is null the method should treat it as a no‑op, exercising the branch that defaults the customizer."
        }
      ]
    },
    {
      "group": "Customizer throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"a\": 1 }"
            },
            {
              "parameter": "format",
              "value": "SimpleStringOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> { throw new RuntimeException(\"Customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"Customizer failure\" propagated to the caller.",
          "description": "The consumer throws an exception; the method should not swallow it, testing the exception‑propagation branch."
        }
      ]
    },
    {
      "group": "Format throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"valid\": true }"
            },
            {
              "parameter": "format",
              "value": "ExceptionThrowingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "executionContext -> {}"
            }
          ],
          "expected": "RuntimeException (or the specific exception thrown by the format) propagated to the caller.",
          "description": "If the OutputFormat implementation throws an exception while producing the result, the method should propagate it, covering the error‑handling branch for format failures."
        }
      ]
    }
  ]
}