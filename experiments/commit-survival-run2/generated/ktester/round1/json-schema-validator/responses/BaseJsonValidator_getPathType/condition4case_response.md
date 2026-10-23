{
    "group": "BaseJsonValidator.getPathType() - Return Correct PathType",
    "cases": [
        {
            "input": [],
            "expected": "PathType.JSON_PATH",
            "description": "When the validator is constructed with PathType.JSON_PATH, getPathType() should return PathType.JSON_PATH."
        },
        {
            "input": [],
            "expected": "PathType.JSON_POINTER",
            "description": "When the validator is constructed with PathType.JSON_POINTER, getPathType() should return PathType.JSON_POINTER."
        },
        {
            "input": [],
            "expected": "PathType.SCHEMA_PATH",
            "description": "When the validator is constructed with PathType.SCHEMA_PATH, getPathType() should return PathType.SCHEMA_PATH."
        },
        {
            "input": [],
            "expected": "PathType.UNKNOWN",
            "description": "When the validator is constructed with PathType.UNKNOWN (or any fallback enum), getPathType() should return PathType.UNKNOWN."
        }
    ]
}