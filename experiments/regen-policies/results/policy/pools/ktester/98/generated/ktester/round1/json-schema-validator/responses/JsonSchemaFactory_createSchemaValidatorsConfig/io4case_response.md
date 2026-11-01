{
    "group": "createSchemaValidatorsConfig_additional",
    "cases": [
        {
            "input": [],
            "expected": "modifying a flag on the first returned instance does not affect a second returned instance (both retain independent default values)",
            "description": "Call createSchemaValidatorsConfig() twice, set failFast=true on the first instance, then assert that failFast on the second instance is still false, confirming that each call returns a fresh, independent object."
        },
        {
            "input": [],
            "expected": "each thread receives a distinct SchemaValidatorsConfig instance (no shared references)",
            "description": "Spawn several threads (e.g., 10) that each invoke createSchemaValidatorsConfig() on the same JsonSchemaFactory instance and store the result in a thread‑local list. After all threads complete, verify that the list contains only unique objects (size equals number of threads)."
        },
        {
            "input": [],
            "expected": "instances created from different JsonSchemaFactory objects are independent",
            "description": "Create two separate JsonSchemaFactory objects, invoke createSchemaValidatorsConfig() on each, modify a flag on the config from the first factory, and assert that the config from the second factory still has the default flag value, proving no cross‑factory sharing."
        },
        {
            "input": [],
            "expected": "non‑null SchemaValidatorsConfig instance obtained via reflection",
            "description": "Use Java reflection to locate the protected method createSchemaValidatorsConfig() on JsonSchemaFactory, make it accessible, invoke it on a JsonSchemaFactory instance, and assert that the returned object is non‑null and of type com.networknt.schema.SchemaValidatorsConfig."
        }
    ]
}