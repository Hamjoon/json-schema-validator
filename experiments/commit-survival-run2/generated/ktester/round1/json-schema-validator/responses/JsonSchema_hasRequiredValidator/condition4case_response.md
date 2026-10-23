{
    "group": "hasRequiredValidator",
    "cases": [
        {
            "input": [],
            "expected": false,
            "description": "When a newly created JsonSchema instance has never had a required validator assigned, the internal requiredValidator field remains null, so hasRequiredValidator() should return false."
        },
        {
            "input": [
                {
                    "parameter": "requiredValidator",
                    "value": "non‑null JsonValidator mock"
                }
            ],
            "expected": true,
            "description": "After injecting a non‑null JsonValidator (e.g., via reflection or a public setter) into the private requiredValidator field, hasRequiredValidator() must recognise the presence of the validator and return true."
        }
    ]
}