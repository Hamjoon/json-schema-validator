{
    "group": "enterDynamicScope(JsonSchema) - with existing parent scope",
    "cases": [
        {
            "input": [
                {
                    "parameter": "containingSchema",
                    "value": "null"
                }
            ],
            "expected": {
                "returnParent": "non‑null Scope instance (the previously pushed scope)",
                "newTopContainingSchema": "same JsonSchema instance that was used to create the previous scope",
                "dynamicScopeSizeIncrease": "by 1 (previous size + 1)"
            },
            "description": "When a parent scope already exists and the argument is null, the method should return the existing parent scope and push a new scope whose containingSchema is taken from the parent (i.e., the same schema as the parent)."
        },
        {
            "input": [
                {
                    "parameter": "containingSchema",
                    "value": "mockNewSchema"
                }
            ],
            "expected": {
                "returnParent": "non‑null Scope instance (the previously pushed scope)",
                "newTopContainingSchema": "mockNewSchema (the schema passed as argument)",
                "dynamicScopeSizeIncrease": "by 1 (previous size + 1)"
            },
            "description": "When a parent scope exists and a non‑null schema is supplied, the method should return the existing parent scope and push a new scope that uses the supplied schema, overriding the parent’s schema."
        }
    ]
}