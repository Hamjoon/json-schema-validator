{
  "testCases": [
    {
      "group": "preloadJsonSchemas - normal operation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List of two mock JsonSchema objects (schemaA, schemaB) where initializeValidators() does nothing"
            }
          ],
          "expected": "initializeValidators() is invoked exactly once on schemaA and exactly once on schemaB; method returns normally without exception",
          "description": "Verifies that the method iterates over the collection and calls initializeValidators() for each schema."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - empty collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "Empty list []"
            }
          ],
          "expected": "No call to initializeValidators(); method returns normally",
          "description": "Ensures that an empty collection does not cause any interaction with JsonSchema objects."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - null element in collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List containing a single null element [null]"
            }
          ],
          "expected": "NullPointerException is thrown when attempting to invoke initializeValidators() on null",
          "description": "Checks that a null element inside the collection triggers a NPE, matching the behaviour of a plain for‑each loop."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - null collection argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown because the method tries to iterate over a null reference",
          "description": "Validates that passing null as the collection argument results in a NPE, as the method does not guard against null."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - schema throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List with one mock JsonSchema (faultySchema) whose initializeValidators() throws RuntimeException(\"init error\")"
            }
          ],
          "expected": "RuntimeException with message \"init error\" propagates out of preloadJsonSchemas",
          "description": "Ensures that if a schema's initializeValidators() throws an unchecked exception, the method does not swallow it and the exception bubbles up."
        }
      ]
    }
  ]
}