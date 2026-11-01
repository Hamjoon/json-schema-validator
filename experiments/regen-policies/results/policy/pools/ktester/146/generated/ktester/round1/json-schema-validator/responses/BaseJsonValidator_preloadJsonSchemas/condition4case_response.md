{
    "group": "preloadJsonSchemas - normal behavior",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "List containing two mock JsonSchema instances (schemaA, schemaB)"
                }
            ],
            "expected": "Both schemaA.initializeValidators() and schemaB.initializeValidators() are invoked exactly once; no exception is thrown.",
            "description": "Verifies that preloadJsonSchemas iterates over a non‑empty collection and calls initializeValidators() on each JsonSchema."
        }
    ]
}