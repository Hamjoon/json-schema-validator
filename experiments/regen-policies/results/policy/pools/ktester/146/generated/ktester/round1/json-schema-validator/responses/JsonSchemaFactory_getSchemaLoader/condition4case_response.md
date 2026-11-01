{
    "group": "Default SchemaLoader retrieval",
    "cases": [
        {
            "input": [],
            "expected": "non‑null instance of com.networknt.schema.resource.DefaultSchemaLoader returned by getSchemaLoader()",
            "description": "Verify that a JsonSchemaFactory created with the default builder returns a non‑null SchemaLoader and that the returned object is an instance of DefaultSchemaLoader."
        },
        {
            "input": [],
            "expected": "the same SchemaLoader instance is returned on subsequent calls (reference equality)",
            "description": "Call getSchemaLoader() twice on the same factory and assert that both references are identical, confirming the getter does not create a new object."
        }
    ]
}