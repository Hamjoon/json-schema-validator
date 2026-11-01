{
  "group": "preloadJsonSchemas behavior",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemas",
          "value": "[]"
        }
      ],
      "expected": "No exception; initializeValidators() is never invoked on any JsonSchema.",
      "description": "Verify that calling preloadJsonSchemas with an empty collection results in a no‑op (the loop body is never executed)."
    },
    {
      "input": [
        {
          "parameter": "schemas",
          "value": "[mockSchema1]"
        }
      ],
      "expected": "initializeValidators() is invoked exactly once on mockSchema1; method returns normally.",
      "description": "Validate that a single JsonSchema element is processed and its initializeValidators() method is called once."
    },
    {
      "input": [
        {
          "parameter": "schemas",
          "value": "[mockSchema1, mockSchema2, mockSchema3]"
        }
      ],
      "expected": "initializeValidators() is invoked exactly once on each of the three mock schemas; method returns normally.",
      "description": "Ensure that the method iterates over all elements in a non‑empty collection and calls initializeValidators() on each."
    },
    {
      "input": [
        {
          "parameter": "schemas",
          "value": "[mockSchemaThrows]"
        }
      ],
      "expected": "RuntimeException thrown by mockSchemaThrows.initializeValidators() propagates out of preloadJsonSchemas.",
      "description": "Confirm that if a schema's initializeValidators() throws an unchecked exception, the method does not catch it and the exception bubbles up."
    },
    {
      "input": [
        {
          "parameter": "schemas",
          "value": "null"
        }
      ],
      "expected": "NullPointerException is thrown when attempting to iterate over a null collection.",
      "description": "Check the method's behavior when a null argument is supplied; the for‑each loop dereferences the collection, resulting in NPE."
    }
  ]
}