{
  "testCases": [
    {
      "group": "BaseJsonValidator.getSchemaLocation.multipleCalls",
      "cases": [
        {
          "input": [],
          "expected": "the same non‑null SchemaLocation instance on every invocation (==) after the validator has been constructed with a known SchemaLocation",
          "description": "Create a concrete subclass of BaseJsonValidator supplying a specific SchemaLocation (e.g. uri='http://example.com/schema.json', pointer='#/properties/age'). Call getSchemaLocation() twice in succession and assert that both returned objects are the identical instance (using ==)."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getSchemaLocation.mutableReference",
      "cases": [
        {
          "input": [],
          "expected": "the same SchemaLocation instance reflecting any mutations performed on the original object",
          "description": "Instantiate a concrete subclass of BaseJsonValidator with a mutable SchemaLocation implementation that allows its URI to be changed after construction. Modify the URI field of the original SchemaLocation object, then call getSchemaLocation() and verify that the returned object is the same instance and that its URI reflects the mutation."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getSchemaLocation.customSubclass",
      "cases": [
        {
          "input": [],
          "expected": "the same instance of a custom SchemaLocation subclass that was supplied to the constructor",
          "description": "Define a custom class `MySchemaLocation` that extends SchemaLocation and adds an extra attribute. Construct a concrete BaseJsonValidator subclass with an instance of `MySchemaLocation`. Invoke getSchemaLocation() and assert that the returned object is the exact same `MySchemaLocation` instance (==) and that the extra attribute is accessible."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getSchemaLocation.threadSafety",
      "cases": [
        {
          "input": [],
          "expected": "the same non‑null SchemaLocation instance returned from concurrent threads without throwing exceptions",
          "description": "Create a concrete BaseJsonValidator with a known SchemaLocation. Spawn several threads that each call getSchemaLocation() and store the result. After all threads complete, verify that every stored reference is identical (==) to the original SchemaLocation and that no thread encountered an exception."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.getSchemaLocation.afterValidation",
      "cases": [
        {
          "input": [],
          "expected": "the same non‑null SchemaLocation instance that was supplied at construction, unchanged by a validation run",
          "description": "Construct a concrete BaseJsonValidator with a specific SchemaLocation and a simple schema. Execute the `validate` method on a valid JSON instance. Afterwards, call getSchemaLocation() and confirm that it still returns the original SchemaLocation instance (==) and that its state has not been altered by the validation process."
        }
      ]
    }
  ]
}