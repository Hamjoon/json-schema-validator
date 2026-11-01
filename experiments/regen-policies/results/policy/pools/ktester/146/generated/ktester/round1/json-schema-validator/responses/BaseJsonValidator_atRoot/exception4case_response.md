{
    "group": "BaseJsonValidator.atRoot() null handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "When the BaseJsonValidator's validationContext field itself is null, atRoot() should throw a NullPointerException because it attempts to call getConfig() on a null reference."
        }
    ]
}