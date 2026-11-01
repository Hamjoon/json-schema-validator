{
    "newTestCases": [
        {
            "group": "Normalization precedence when multiple draft substrings present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "https://example.com/draft-07/path/draft-06/schema.json"
                        }
                    ],
                    "expected": "http://json-schema.org/draft-07/schema#",
                    "description": "The string contains both \"/draft-07/\" and \"/draft-06/\". The method checks for \"/draft-07/\" first, so it should normalize to the V7 meta‑schema."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "https://example.com/draft/2019-09/path/draft-07/schema"
                        }
                    ],
                    "expected": "http://json-schema.org/draft-07/schema#",
                    "description": "Both \"/draft/2019-09/\" and \"/draft-07/\" appear; the earlier check for \"/draft-07/\" wins, resulting in V7 normalization."
                }
            ]
        },
        {
            "group": "Case sensitivity of draft marker",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "HTTPS://json-schema.org/DRAFT-07/schema#"
                        }
                    ],
                    "expected": "HTTPS://json-schema.org/DRAFT-07/schema#",
                    "description": "The method uses case‑sensitive `contains`. Because the marker \"://json-schema.org/draft\" is not found in the upper‑case form, the id is returned unchanged."
                }
            ]
        },
        {
            "group": "Missing trailing slash after version substring",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "https://example.com/draft-07schema"
                        }
                    ],
                    "expected": "https://example.com/draft-07schema",
                    "description": "The substring \"/draft-07/\" is not present (no trailing slash), so no normalization occurs."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "https://example.com/draft-07"
                        }
                    ],
                    "expected": "https://example.com/draft-07",
                    "description": "Again, the required \"/draft-07/\" pattern is missing; the method should leave the id untouched."
                }
            ]
        },
        {
            "group": "Draft marker without required \"://\" prefix",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "http://example.com/json-schema.org/draft-07/schema"
                        }
                    ],
                    "expected": "http://example.com/json-schema.org/draft-07/schema",
                    "description": "The outer check looks for \"://json-schema.org/draft\". Because the string contains \"json-schema.org/draft\" but not the exact prefix, no normalization is performed."
                }
            ]
        },
        {
            "group": "Normalization with non‑http scheme (ftp)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "ftp://json-schema.org/draft-07/schema"
                        }
                    ],
                    "expected": "http://json-schema.org/draft-07/schema#",
                    "description": "The scheme does not matter; the presence of \"://json-schema.org/draft\" and \"/draft-07/\" triggers normalization to the V7 meta‑schema."
                }
            ]
        },
        {
            "group": "Normalization with query parameters and fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "http://json-schema.org/draft-07/schema?foo=bar#section"
                        }
                    ],
                    "expected": "http://json-schema.org/draft-07/schema#",
                    "description": "Even though the original id contains a query string and a fragment, the method replaces the whole id with the canonical V7 identifier."
                }
            ]
        },
        {
            "group": "Already normalized but with extra fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "http://json-schema.org/draft-07/schema#extra"
                        }
                    ],
                    "expected": "http://json-schema.org/draft-07/schema#",
                    "description": "The id does not exactly match any VersionFlag (extra fragment), so the unnormalized branch runs and normalizes it to the standard V7 identifier."
                }
            ]
        },
        {
            "group": "Empty string containing draft marker but no specific version",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "http://json-schema.org/draft/"
                        }
                    ],
                    "expected": "http://json-schema.org/draft/",
                    "description": "The string contains the draft marker but none of the specific version substrings, therefore the method returns the original id unchanged."
                }
            ]
        }
    ]
}