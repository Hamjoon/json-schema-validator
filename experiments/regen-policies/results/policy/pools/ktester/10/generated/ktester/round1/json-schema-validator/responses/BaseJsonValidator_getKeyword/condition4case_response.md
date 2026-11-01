{
    "group": "BaseJsonValidator#getKeyword - normal operation",
    "cases": [
        {
            "input": [],
            "expected": "type",
            "description": "When the concrete validator sets the protected `keyword` field to a Keyword instance whose value is \"type\", `getKeyword()` should return the exact string \"type\"."
        },
        {
            "input": [],
            "expected": "minimum",
            "description": "When the concrete validator sets the protected `keyword` field to a Keyword instance whose value is \"minimum\", `getKeyword()` should return \"minimum\"."
        },
        {
            "input": [],
            "expected": "pattern",
            "description": "When the concrete validator sets the protected `keyword` field to a Keyword instance whose value is \"pattern\", `getKeyword()` should return \"pattern\"."
        }
    ]
}