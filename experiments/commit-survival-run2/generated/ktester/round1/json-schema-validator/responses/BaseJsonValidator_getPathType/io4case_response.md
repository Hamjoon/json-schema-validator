{
    "groups": [
        {
            "group": "BaseJsonValidator.getPathType() - Null handling and default fallback",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "pathType",
                            "value": null
                        }
                    ],
                    "expected": "PathType.UNKNOWN",
                    "description": "If the validator is constructed with a null PathType (e.g., via a constructor that accepts a nullable argument), getPathType() should safely fall back to PathType.UNKNOWN rather than throwing NPE."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getPathType() - Immutability across operations",
            "cases": [
                {
                    "input": [],
                    "expected": "PathType.JSON_PATH",
                    "description": "When a validator is created with PathType.JSON_PATH, invoking other public methods (e.g., validate()) must not change the stored pathType; getPathType() should still return JSON_PATH."
                },
                {
                    "input": [],
                    "expected": "PathType.SCHEMA_PATH",
                    "description": "When a validator is created with PathType.SCHEMA_PATH, after calling preloadJsonSchemas(...) the pathType must remain unchanged; getPathType() should still return SCHEMA_PATH."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getPathType() - Subclass behavior",
            "cases": [
                {
                    "input": [],
                    "expected": "PathType.JSON_POINTER",
                    "description": "A concrete subclass that does not override getPathType() should inherit the base implementation; when constructed with PathType.JSON_POINTER, calling getPathType() on the subclass instance must return JSON_POINTER."
                },
                {
                    "input": [],
                    "expected": "PathType.JSON_POINTER",
                    "description": "Even if the subclass defines its own method named getPathType() (shadowing the base method), invoking the inherited BaseJsonValidator#getPathType() via super.getPathType() must still return the original PathType (JSON_POINTER)."
                }
            ]
        }
    ]
}