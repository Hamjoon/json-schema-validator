{
  "testCases": [
    {
      "group": "preloadJsonSchemas - duplicate schema instances",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List containing the same mock JsonSchema instance twice"
            }
          ],
          "expected": "initializeValidators() is invoked exactly two times on the single mock JsonSchema instance; method returns normally without exception",
          "description": "Verifies that the method does not deduplicate collection elements – each occurrence, even if referring to the same object, triggers a separate call to initializeValidators()."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - collection type variations",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "Set containing three distinct mock JsonSchema objects"
            }
          ],
          "expected": "initializeValidators() is invoked once on each of the three schemas; method returns normally; call order is unspecified because Set iteration order is undefined",
          "description": "Ensures that the method works with a Set (any Collection implementation) and processes every element regardless of ordering."
        },
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "Unmodifiable list (Collections.unmodifiableList) with two mock JsonSchema objects"
            }
          ],
          "expected": "initializeValidators() is invoked once on each schema; method returns normally without attempting to modify the list",
          "description": "Confirms that the method does not modify the supplied collection, allowing it to work with immutable collections."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - side‑effect verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "List with three mock JsonSchema objects whose initializeValidators() increments a shared counter"
            }
          ],
          "expected": "Shared counter value equals 3 after method execution; no exception is thrown",
          "description": "Validates that any side‑effects performed inside initializeValidators() are propagated, i.e., the method simply invokes the callbacks."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - concurrent modification during iteration",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "ArrayList with two mock JsonSchema objects; the first object's initializeValidators() adds a third mock schema to the same list"
            }
          ],
          "expected": "ConcurrentModificationException is thrown and propagates out of preloadJsonSchemas",
          "description": "Tests the method's behavior when a schema mutates the collection while it is being iterated – the standard for‑each loop should detect the structural change and raise CME."
        }
      ]
    },
    {
      "group": "preloadJsonSchemas - empty immutable collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemas",
              "value": "Collections.emptyList() (immutable empty collection)"
            }
          ],
          "expected": "Method returns normally; initializeValidators() is never called",
          "description": "Ensures that an immutable empty collection is treated the same as a regular empty list – the method performs a no‑op without throwing."
        }
      ]
    }
  ]
}