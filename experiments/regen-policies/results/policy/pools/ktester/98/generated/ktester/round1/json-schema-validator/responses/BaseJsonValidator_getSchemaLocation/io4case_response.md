{
  "group": "BaseJsonValidator.getSchemaLocation.additional",
  "cases": [
    {
      "input": [],
      "expected": "the exact same SchemaLocation instance on every call (reference equality)",
      "description": "Create a concrete subclass of BaseJsonValidator with a non‑null SchemaLocation. Invoke getSchemaLocation() twice and assert that both returned references are == (the same object). This verifies that the method does not create a new object on each call."
    },
    {
      "input": [],
      "expected": "the SchemaLocation instance reflects modifications made through the returned reference",
      "description": "Instantiate the validator with a mutable SchemaLocation (e.g., a custom implementation exposing a setUri method). Call getSchemaLocation(), change its internal URI, then call getSchemaLocation() again and verify that the updated URI is visible, proving that the method returns the actual stored instance, not a copy."
    },
    {
      "input": [],
      "expected": "the custom subclass instance of SchemaLocation that was injected",
      "description": "Define a subclass of SchemaLocation (e.g., MySchemaLocation) that adds extra behavior. Pass an instance of this subclass to the validator constructor. getSchemaLocation() should return the same MySchemaLocation object, confirming that the method preserves subclass types."
    },
    {
      "input": [],
      "expected": "the same SchemaLocation instance as before validation",
      "description": "Create a validator with a concrete SchemaLocation, run a simple validation (e.g., validate an empty JSON node against a trivial schema) and then call getSchemaLocation(). The returned reference must be identical to the one set at construction, showing that validation does not alter the field."
    },
    {
      "input": [],
      "expected": "the spy SchemaLocation object injected during construction",
      "description": "Use Mockito.spy() to create a spy of a real SchemaLocation, inject it into the validator, and call getSchemaLocation(). Verify that the returned object is the same spy and that no unexpected interactions (e.g., extra method calls) occurred during the getter execution."
    }
  ]
}