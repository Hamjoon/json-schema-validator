{
    "group": "BaseJsonValidator.getSchemaLocation",
    "cases": [
        {
            "input": [],
            "expected": "the same non‑null SchemaLocation instance that was supplied to the validator’s constructor",
            "description": "Create a concrete subclass of BaseJsonValidator with a known SchemaLocation (e.g. uri='http://example.com/schema.json', pointer='#/properties/name'). Call getSchemaLocation() and verify that the returned object is the exact same instance (==) as the one passed during construction."
        },
        {
            "input": [],
            "expected": "null",
            "description": "Instantiate a concrete subclass of BaseJsonValidator without setting a SchemaLocation (or explicitly set it to null). Call getSchemaLocation() and verify that the method returns null and does not throw any exception."
        }
    ]
}