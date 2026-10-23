{
    "group": "normalizeMetaSchemaUri - illegal argument handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "u",
                    "value": "http://exa mple.com/path"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Host contains a space, which is illegal for a URI component; the constructor throws IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "u",
                    "value": "http://user:pa ss@example.com"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "UserInfo contains a space, causing the URI constructor to throw IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "u",
                    "value": "http://example.com/invalid path"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Path contains an unescaped space, which is illegal; the URI constructor throws IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "u",
                    "value": "http://example.com:-1"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Port number is negative, which is outside the valid range; the URI constructor throws IllegalArgumentException."
        }
    ]
}