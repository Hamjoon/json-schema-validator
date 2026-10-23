{
    "test_cases": [
        {
            "group": "Null PathType leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentPath",
                            "value": "/test"
                        },
                        {
                            "parameter": "token",
                            "value": "field"
                        },
                        {
                            "parameter": "pathType",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the BaseJsonValidator's internal pathType field is null, invoking atPath should throw a NullPointerException because the method attempts to call append on a null reference."
                }
            ]
        },
        {
            "group": "Null token parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentPath",
                            "value": "/users"
                        },
                        {
                            "parameter": "token",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the token argument is null, PathType.append (called inside atPath) is expected to throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Both currentPath and token null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentPath",
                            "value": null
                        },
                        {
                            "parameter": "token",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When both currentPath and token are null, atPath should propagate a NullPointerException from the underlying PathType.append implementation."
                }
            ]
        },
        {
            "group": "Invalid token for JSON_POINTER (contains illegal character)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentPath",
                            "value": "/data"
                        },
                        {
                            "parameter": "token",
                            "value": "invalid\u0000char"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A token containing a null character is not permitted in a JSON Pointer segment; PathType.append should detect this and throw an IllegalArgumentException."
                }
            ]
        }
    ]
}