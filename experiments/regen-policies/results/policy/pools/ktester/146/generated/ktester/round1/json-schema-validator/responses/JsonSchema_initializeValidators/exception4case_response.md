{
    "group": "initializeValidators - validator list contains null element",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException propagated and validatorsLoaded stays false",
            "description": "Force getValidators() to return a list that includes a null entry. The for‑each loop will attempt to invoke preloadJsonSchema() on the null element, causing a NullPointerException. The method should not catch it, and validatorsLoaded must remain false."
        }
    ]
}