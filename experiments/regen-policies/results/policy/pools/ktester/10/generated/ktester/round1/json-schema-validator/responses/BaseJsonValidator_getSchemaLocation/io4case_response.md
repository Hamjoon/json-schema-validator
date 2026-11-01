{
    "group": "BaseJsonValidator.getSchemaLocation",
    "cases": [
        {
            "input": [],
            "expected": "returns the exact same SchemaLocation object on successive calls (reference equality)",
            "description": "Create a concrete subclass of BaseJsonValidator, inject a non‑null SchemaLocation (e.g., via constructor or reflection), invoke getSchemaLocation() twice and assert that the two returned references are identical (==). This verifies that the getter does not create a new object on each call."
        },
        {
            "input": [],
            "expected": "returns the subclass instance that was assigned to the internal field",
            "description": "Define a class CustomSchemaLocation that extends SchemaLocation, assign an instance of it to the validator's private schemaLocation field (using reflection), then call getSchemaLocation() and assert that the returned object is an instance of CustomSchemaLocation. This ensures that the method does not cast or wrap the field."
        },
        {
            "input": [],
            "expected": "returns the newly set SchemaLocation after the field is changed via reflection",
            "description": "Construct a validator with an initial SchemaLocation, then replace the private schemaLocation field with a different SchemaLocation instance using reflection. Call getSchemaLocation() and verify that the returned reference matches the newly set instance, confirming that the getter reflects the current field value."
        },
        {
            "input": [],
            "expected": "returns the same SchemaLocation instance even after invoking other public methods (e.g., validate)",
            "description": "Instantiate a concrete validator, set a known SchemaLocation, invoke a harmless public method such as toString() or a stubbed validate(...) call, and then call getSchemaLocation(). The returned reference should still be the original SchemaLocation, demonstrating that unrelated operations do not alter the field."
        }
    ]
}