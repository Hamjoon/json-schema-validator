{
    "group": "BaseJsonValidator.getSchemaPath",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": " "
                }
            ],
            "expected": " ",
            "description": "A single whitespace character should be returned unchanged, confirming that the method does not trim or modify whitespace."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "\t\n"
                }
            ],
            "expected": "\t\n",
            "description": "Tab and newline characters are preserved exactly, testing that control characters are not altered."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/naïve"
                }
            ],
            "expected": "/properties/naïve",
            "description": "Unicode characters (e.g., ‘ï’) inside the path are returned verbatim, verifying UTF‑8 handling."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/\\name"
                }
            ],
            "expected": "/properties/\\name",
            "description": "Backslash characters are part of the stored string and should be returned unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/~0name"
                }
            ],
            "expected": "/properties/~0name",
            "description": "Escaped tilde (`~0`) representing a literal '~' must be returned as‑is, testing handling of JSON‑Pointer escape sequences."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/~1name"
                }
            ],
            "expected": "/properties/~1name",
            "description": "Escaped forward‑slash (`~1`) representing a literal '/' is returned unchanged, covering another JSON‑Pointer escape case."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/"
                }
            ],
            "expected": "/",
            "description": "Root pointer '/' should be returned exactly, confirming that a minimal non‑empty path is handled correctly."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/a" 
                }
            ],
            "expected": "/a",
            "description": "Simple single‑character property name verifies basic functionality."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/very/long/path/" 
                }
            ],
            "expected": "/very/long/path/",
            "description": "A relatively long path (multiple segments) should be returned unchanged, testing that length does not affect behavior."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/name?query=param"
                }
            ],
            "expected": "/properties/name?query=param",
            "description": "Path containing a query‑string‑like segment is returned verbatim, ensuring that characters beyond standard JSON‑Pointer syntax are not stripped."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/name#fragment"
                }
            ],
            "expected": "/properties/name#fragment",
            "description": "Path with a fragment identifier is returned unchanged, confirming that the method does not interpret or remove such characters."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/a" 
                }
            ],
            "expected": "/a",
            "description": "Repeated simple case to illustrate that the method consistently returns the exact stored value."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/name/0/1/2/3/4/5/6/7/8/9"
                }
            ],
            "expected": "/properties/name/0/1/2/3/4/5/6/7/8/9",
            "description": "Path with many numeric segments tests that the method handles deep hierarchical pointers without modification."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/name~0~1"
                }
            ],
            "expected": "/properties/name~0~1",
            "description": "Combination of both escape sequences (`~0` and `~1`) in a single token should be preserved exactly."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/😀"
                }
            ],
            "expected": "/properties/😀",
            "description": "Emoji character inside the path verifies that surrogate pairs are handled correctly."
        }
    ]
}