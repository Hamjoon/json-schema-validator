{
    "group": "initializeValidators - null validators list",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException is thrown; validatorsLoaded remains false",
            "description": "When getValidators() returns null, the for‑each loop attempts to iterate over a null reference, causing a NullPointerException before any preload is attempted. The validatorsLoaded flag must stay false so a later retry can succeed."
        }
    ]
}