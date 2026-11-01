{
    "group": "BaseJsonValidator.atRoot()",
    "cases": [
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.JSON"
                }
            ],
            "expected": "Returns a non‑null JsonNodePath whose internal PathType equals PathType.JSON",
            "description": "Validate that atRoot() creates a JsonNodePath using the validator's PathType when the field is set to JSON."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.SCHEMA"
                }
            ],
            "expected": "Returns a non‑null JsonNodePath whose internal PathType equals PathType.SCHEMA",
            "description": "Validate that atRoot() correctly propagates a different PathType (SCHEMA) to the created JsonNodePath."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "null"
                }
            ],
            "expected": "Returns a non‑null JsonNodePath whose internal PathType is null (or throws NullPointerException if JsonNodePath does not accept null)",
            "description": "Edge case – verify the behaviour when the validator's pathType field is null. The method should still construct a JsonNodePath (or fail with NPE, which is the expected outcome)."
        }
    ]
}