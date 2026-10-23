{
    "testCases": [
        {
            "group": "Enter With Already Associated Schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "containingSchema",
                            "value": "mockJsonSchemaAssociatedWithOtherContext"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Pass a JsonSchema instance that is already linked to a different CollectorContext (simulated via reflection or a stub that reports an existing association). The method should reject re‑using the same schema in another context and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Enter When Internal Collector Map Is Null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Create a CollectorContext, force its internal collector map (or similar required field) to null using reflection, then invoke enterDynamicScope(). The method is expected to attempt to access the map and consequently throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Enter After Context Has Been Marked As Finished",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "Assume CollectorContext provides a way to mark the context as finished (e.g., a hypothetical close() or freeze() method). After invoking that method, call enterDynamicScope(). The method should detect the illegal state and throw IllegalStateException."
                }
            ]
        }
    ]
}