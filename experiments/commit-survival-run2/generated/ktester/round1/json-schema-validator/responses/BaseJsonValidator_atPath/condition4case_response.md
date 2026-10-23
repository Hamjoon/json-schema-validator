{
    "group": "Appending paths with slash separator (PathType.SLASH)",
    "cases": [
        {
            "input": [
                { "parameter": "currentPath", "value": "/users/123" },
                { "parameter": "token", "value": "address" }
            ],
            "expected": "/users/123/address",
            "description": "When the current path ends with a slash‑separated segment and the token is a normal field name, the slash PathType should concatenate them with a '/' separator."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": "" },
                { "parameter": "token", "value": "root" }
            ],
            "expected": "/root",
            "description": "If the current path is empty, the slash PathType should treat the token as the first segment and prepend a leading '/'."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": "/items" },
                { "parameter": "token", "value": "" }
            ],
            "expected": "/items/",
            "description": "When the token is an empty string, the slash PathType should still append a '/' to indicate an empty segment."
        }
    ]
}