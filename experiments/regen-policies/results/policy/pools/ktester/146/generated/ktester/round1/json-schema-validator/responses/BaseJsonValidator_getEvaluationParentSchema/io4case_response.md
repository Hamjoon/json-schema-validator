{
    "group": "Dynamic Parent Schema Retrieval",
    "cases": [
        {
            "input": [],
            "expected": "parentSchemaInstanceA",
            "description": "evaluationParentSchema is null and the validator’s getParentSchema() initially returns a specific JsonSchema instance (A). The method should return that instance. This verifies the fallback path works for the first call."
        },
        {
            "input": [],
            "expected": "parentSchemaInstanceB",
            "description": "After the first call, the parent schema reference inside the validator is changed to a different JsonSchema instance (B). Because evaluationParentSchema is still null, a subsequent call to getEvaluationParentSchema() should now return the new parent (B). This confirms that the method does not cache the fallback value."
        }
    ]
}