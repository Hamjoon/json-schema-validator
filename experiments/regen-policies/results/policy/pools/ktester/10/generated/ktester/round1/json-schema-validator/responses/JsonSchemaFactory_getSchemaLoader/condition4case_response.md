{
    "group": "JsonSchemaFactory.getSchemaLoader",
    "cases": [
        {
            "input": [],
            "expected": "the same non‑null SchemaLoader instance that was injected into the factory",
            "description": "Create a JsonSchemaFactory (via its Builder or reflection) with a mock SchemaLoader, invoke getSchemaLoader() and verify that the returned object is the exact instance that was supplied."
        },
        {
            "input": [],
            "expected": "null",
            "description": "Using reflection, set the private final field `schemaLoader` of a JsonSchemaFactory instance to null, then call getSchemaLoader() and confirm that it returns null. This checks the method’s behaviour when the internal field is not initialised."
        },
        {
            "input": [],
            "expected": "the same SchemaLoader instance on subsequent calls",
            "description": "Call getSchemaLoader() twice on the same JsonSchemaFactory and assert that both calls return the identical object (reference equality). This ensures the method does not create a new instance on each invocation."
        }
    ]
}