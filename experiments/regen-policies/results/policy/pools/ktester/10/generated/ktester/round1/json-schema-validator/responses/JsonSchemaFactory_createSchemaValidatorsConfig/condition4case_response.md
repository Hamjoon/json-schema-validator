{
    "group": "createSchemaValidatorsConfig",
    "cases": [
        {
            "input": [],
            "expected": "non‑null instance of SchemaValidatorsConfig",
            "description": "Calling createSchemaValidatorsConfig() should return a non‑null SchemaValidatorsConfig object."
        },
        {
            "input": [],
            "expected": "two distinct instances (different references) on successive calls",
            "description": "Each invocation must create a fresh SchemaValidatorsConfig; the returned objects must not be the same reference."
        },
        {
            "input": [],
            "expected": "default configuration values (e.g., failFast = false, typeLoose = false, etc.)",
            "description": "The newly created SchemaValidatorsConfig should contain the library's default settings. Verify that all boolean/enum fields hold their documented default values."
        }
    ]
}