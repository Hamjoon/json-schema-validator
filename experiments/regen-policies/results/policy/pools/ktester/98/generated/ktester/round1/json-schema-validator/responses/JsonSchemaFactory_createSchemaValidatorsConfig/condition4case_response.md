{
    "group": "createSchemaValidatorsConfig",
    "cases": [
        {
            "input": [],
            "expected": "non‑null instance of com.networknt.schema.SchemaValidatorsConfig",
            "description": "Verify that the protected method createSchemaValidatorsConfig() returns a non‑null SchemaValidatorsConfig object when invoked."
        },
        {
            "input": [],
            "expected": "two distinct SchemaValidatorsConfig instances on successive calls",
            "description": "Call createSchemaValidatorsConfig() twice and assert that the returned objects are not the same reference, confirming a fresh instance is created each time."
        },
        {
            "input": [],
            "expected": "default configuration values (e.g., failFast == false, typeLoose == false, readOnly == false, ...)",
            "description": "Inspect the newly created SchemaValidatorsConfig and verify that all its configurable flags retain the library‑defined default values."
        }
    ]
}