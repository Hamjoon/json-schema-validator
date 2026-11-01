{
    "group": "BaseJsonValidator.atRoot() - additional scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.JSON"
                }
            ],
            "expected": "Two successive calls to atRoot() return JsonNodePath objects that are equal (equals() == true) but are distinct instances (== false).",
            "description": "Ensures that atRoot() creates a fresh JsonNodePath each time while preserving the same logical root location."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.JSON"
                }
            ],
            "expected": "The JsonNodePath returned by atRoot() represents the root location – its string form is \"$\" (or empty string depending on implementation) and it contains no path segments.",
            "description": "Verifies that the path created for the root has the expected textual representation and contains no child segments."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.JSON"
                }
            ],
            "expected": "Modifying the first JsonNodePath (e.g., appending a segment) does not affect a new JsonNodePath obtained from a subsequent atRoot() call; the new path remains the root.",
            "description": "Checks that the JsonNodePath instances returned by atRoot() are independent and that internal validator state is not mutated by external changes to the path."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.SCHEMA"
                }
            ],
            "expected": "The JsonNodePath returned by atRoot() for PathType.SCHEMA also represents the root (string form \"$\" or equivalent) and contains no segments.",
            "description": "Confirms that root‑path semantics are identical for the SCHEMA PathType, not only for JSON."
        },
        {
            "input": [
                {
                    "parameter": "pathType",
                    "value": "PathType.JSON"
                }
            ],
            "expected": "When a concrete subclass of BaseJsonValidator is instantiated (e.g., a minimal TestValidator), atRoot() still returns a non‑null JsonNodePath with the correct PathType.",
            "description": "Validates that the abstract nature of BaseJsonValidator does not interfere with atRoot() behavior when used through a concrete implementation."
        }
    ]
}