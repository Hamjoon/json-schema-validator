{
  "testCases": [
    {
      "group": "preloadJsonSchemas - normal operation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List containing three mock JsonSchema objects"
            }
          ],
          "expected": "initializeValidators() is invoked exactly once on each of the three JsonSchema instances; method returns normally without exception",
          "description": "Verifies that preloadJsonSchemas iterates over a non‑empty collection and calls initializeValidators() on every schema."
        },
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "Empty list []"
            }
          ],
          "expected": "Method returns normally; initializeValidators() is never called",
          "description": "Ensures that an empty collection results in a no‑op – the loop body is never executed."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List containing a null element [null]"
            }
          ],
          "expected": "NullPointerException is thrown when attempting to invoke initializeValidators() on a null reference",
          "description": "Confirms that the method does not guard against null elements; a null schema leads to NPE."
        },
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown because the method tries to iterate over a null collection",
          "description": "Validates that passing a null collection results in NPE (the method does not perform a null‑check)."
        },
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List with one mock JsonSchema whose initializeValidators() throws RuntimeException"
            }
          ],
          "expected": "RuntimeException propagates out of preloadJsonSchemas; no further schemas are processed",
          "description": "Checks that exceptions thrown by a schema’s initializeValidators() are not caught and abort processing."
        }
      ]
    }
  ]
}