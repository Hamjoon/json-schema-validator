{
    "group": "nullValidationContextThrowsNPE",
    "cases": [
        {
            "input": [
                {
                    "parameter": "config",
                    "value": "ValidConfig"
                }
            ],
            "expected": "NullPointerException is thrown",
            "description": "If the JsonSchema instance has a null ValidationContext (e.g., created via reflection or a faulty factory), calling withConfig will attempt to invoke getValidationContext().getMetaSchema() and result in a NullPointerException."
        }
    ]
}