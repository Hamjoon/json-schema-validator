{
    "testCases": [
        {
            "group": "Non‑String Object with toString",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "Boolean.TRUE"
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"true\") – i.e., the child node whose field name is the string representation of the Boolean value",
                    "description": "The argument is not a Number, so the method falls back to the else‑branch, calls toString() on the Boolean, and looks up the resulting key \"true\" in the schema node."
                }
            ]
        },
        {
            "group": "Double Number Index (truncation)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": 2.9
                        }
                    ],
                    "expected": "JsonNode returned by node.get(2) – the element at index 2 after the double is truncated to an int",
                    "description": "When a Number that is not an integral type is supplied, the method uses intValue(), which truncates the fractional part before indexing the array."
                }
            ]
        },
        {
            "group": "Negative Number Index",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": -1
                        }
                    ],
                    "expected": "null (no element at a negative index)",
                    "description": "A negative integer is passed; JsonNode.get(-1) returns null, verifying that the method does not perform bounds checking itself."
                }
            ]
        },
        {
            "group": "Out‑of‑Bounds Index",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": 999
                        }
                    ],
                    "expected": "null (index larger than the array size)",
                    "description": "The supplied index exceeds the size of the underlying array node; the method should simply forward the call to node.get(999) which yields null."
                }
            ]
        },
        {
            "group": "Empty String Property",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": ""
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"\") – typically null unless the schema explicitly contains an empty‑named field",
                    "description": "An empty string does not trigger any special‑character handling; it is passed unchanged to node.get(\"\")."
                }
            ]
        },
        {
            "group": "Multiple Tilde Replacements in One String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "a~1b~0c"
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"a/b~c\") – after '~1' → '/' and '~0' → '~' replacements",
                    "description": "The string contains both \"~1\" and \"~0\" tokens. The method first replaces \"~1\" with '/', then \"~0\" with '~', yielding the key \"a/b~c\"."
                }
            ]
        },
        {
            "group": "Percent Decoding Produces Tilde After Decoding (order test)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "a%7E1b"
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"a~1b\") – percent decoding occurs after the '~' check, so no further tilde‑replacement is applied",
                    "description": "The original string contains a percent‑encoded '~' (%7E). Because the method checks for '~' before decoding, the '~' token is not replaced; after decoding the string becomes \"a~1b\" and is used as‑is."
                }
            ]
        },
        {
            "group": "String with Tilde but No Replacement Tokens and Percent Encoding",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "no~tilde%20space"
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"no~tilde space\") – '~' check runs (no token replacement) then percent decoding turns \"%20\" into a space",
                    "description": "The string contains a '~' character that does not form a replacement token, and also contains a percent‑encoded space. The method runs the '~' branch (making no changes) and then decodes \"%20\" to a space."
                }
            ]
        },
        {
            "group": "Object with Custom toString Returning Escaped Pointer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "new Object(){ public String toString(){ return \"x~1y%2Fz\"; } }"
                        }
                    ],
                    "expected": "JsonNode returned by node.get(\"x/y/z\") – after applying both '~' replacement and percent decoding on the custom string",
                    "description": "An arbitrary object whose toString() yields a JSON‑pointer‑escaped string. The method should treat it like any other non‑Number, performing the '~' and '%' processing steps."
                }
            ]
        }
    ]
}