{
    "group": "atRoot() - normal behavior with different PathType implementations",
    "cases": [
        {
            "input": [],
            "expected": "\"$\"",
            "description": "When the validator is constructed with a PathType whose getRoot() returns the JSONPath root symbol \"$\", atRoot() should return \"$\"."
        },
        {
            "input": [],
            "expected": "\"\"",
            "description": "When the validator is constructed with a PathType whose getRoot() returns an empty string (typical for JSON Pointer), atRoot() should return an empty string."
        },
        {
            "input": [],
            "expected": "\"/\"",
            "description": "When the validator is constructed with a PathType whose getRoot() returns \"/\" (another possible root for JSON Pointer), atRoot() should return \"/\"."
        }
    ]
}