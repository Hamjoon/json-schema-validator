{
    "group": "BaseJsonValidator.getKeyword()",
    "cases": [
        {
            "input": [],
            "expected": "\"type\"",
            "description": "Validate that getKeyword() returns the string value of a normal keyword (e.g., Keyword.TYPE) when the validator is constructed with that keyword."
        },
        {
            "input": [],
            "expected": "\"properties\"",
            "description": "Validate that getKeyword() correctly returns a different keyword value (e.g., Keyword.PROPERTIES) to ensure the method does not hard‑code any particular string."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Validate that getKeyword() throws a NullPointerException when the internal `keyword` field is null (simulating a mis‑configured validator)."
        }
    ]
}