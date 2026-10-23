{
    "group": "Appending paths with JSON Pointer separator (PathType.JSON_POINTER)",
    "cases": [
        {
            "input": [
                { "parameter": "currentPath", "value": "/data" },
                { "parameter": "token", "value": "items" }
            ],
            "expected": "/data/items",
            "description": "When using JSON_POINTER, a normal token is appended with a '/' separator."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": "" },
                { "parameter": "token", "value": "root" }
            ],
            "expected": "/root",
            "description": "An empty current path is treated as the root; the token becomes the first segment prefixed with '/'."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": "/array" },
                { "parameter": "token", "value": "" }
            ],
            "expected": "/array/",
            "description": "Appending an empty token results in a trailing '/' to indicate an empty segment."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": "/a~b" },
                { "parameter": "token", "value": "c/d" }
            ],
            "expected": "/a~0b/c~1d",
            "description": "Tokens containing '~' and '/' are escaped according to JSON Pointer rules (~ → ~0, / → ~1) before concatenation."
        },
        {
            "input": [
                { "parameter": "currentPath", "value": null },
                { "parameter": "token", "value": "first" }
            ],
            "expected": "/first",
            "description": "A null current path is interpreted as empty, so the token becomes the first segment with a leading '/'."
        }
    ]
}