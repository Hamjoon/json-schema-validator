{
    "group": "BaseJsonValidator.atRoot() null handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validationContext.getConfig()",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "When the ValidationContext's Config is null, atRoot() should throw a NullPointerException because it attempts to call getPathType() on a null reference."
        },
        {
            "input": [
                {
                    "parameter": "validationContext.getConfig().getPathType()",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "When the Config returns a null PathType, atRoot() should throw a NullPointerException while evaluating the equals checks."
        }
    ]
}