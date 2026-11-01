{
    "group": "UnresolvedReference",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "jsonSchemaWithUnresolvableRefStream"
                },
                {
                    "parameter": "config",
                    "value": "defaultConfig"
                }
            ],
            "expected": "JsonSchemaException (or RuntimeException) indicating that $ref could not be resolved",
            "description": "The InputStream contains a valid JSON schema that includes a $ref to a non‑existent external schema. The factory should attempt to resolve the reference during loading and throw an exception because the reference cannot be found."
        }
    ]
}