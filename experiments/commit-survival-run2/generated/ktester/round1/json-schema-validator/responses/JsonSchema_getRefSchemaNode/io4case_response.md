{
    "testCases": [
        {
            "group": "Reference equal to \"#\" – returns original schema node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#"
                        }
                    ],
                    "expected": "Returns the original schema node obtained from findAncestor().getSchemaNode() because the reference does not satisfy any of the conditional branches (length is not > 1).",
                    "description": "The reference consists of only a hash character. The method should skip the \"#/\" and \"#\" fragment handling blocks and fall through, returning the unchanged original node."
                }
            ]
        },
        {
            "group": "Empty string reference – returns original schema node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": ""
                        }
                    ],
                    "expected": "Returns the original schema node (the node obtained from findAncestor().getSchemaNode()).",
                    "description": "An empty string does not match any prefix checks, so the method should simply return the original node."
                }
            ]
        },
        {
            "group": "JSON Pointer starting with \"/\" – normal (non‑missing) node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "/properties/age"
                        }
                    ],
                    "expected": "Returns the JsonNode located at \"/properties/age\" (non‑missing node). The method should not invoke handleNullNode.",
                    "description": "The reference begins directly with \"/\". After URL decoding the pointer, node.at(\"/properties/age\") returns a valid JsonNode, so the method returns it directly."
                }
            ]
        },
        {
            "group": "Reference starting with \"urn:\" but exactly 4 characters – falls back to original node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "urn:"
                        }
                    ],
                    "expected": "Returns the original schema node because the length check (ref.length() > 4) fails, so the branch is not taken.",
                    "description": "Although the string starts with \"urn:\", its length is not greater than 4, therefore the method treats it as a plain reference and returns the original node."
                }
            ]
        },
        {
            "group": "JSON Pointer with \"#/\" prefix that resolves to a missing node – fallback via handleNullNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#/nonexistent"
                        }
                    ],
                    "expected": "Returns the fallback JsonNode produced by handleNullNode(ref, schema) because node.at(\"/nonexistent\") yields a MissingNode.",
                    "description": "The reference starts with \"#/\". After stripping the leading \"#\" the pointer becomes \"/nonexistent\". The call to node.at returns a MissingNode, triggering the fallback logic."
                }
            ]
        },
        {
            "group": "JSON Pointer with URL‑encoded slash (%2F) inside the path",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#/components%2Fschemas%2FUser"
                        }
                    ],
                    "expected": "Returns the JsonNode located at \"/components/schemas/User\" after URL decoding the \"%2F\" sequences.",
                    "description": "The reference begins with \"#/\" and contains URL‑encoded forward slashes. After decoding, the pointer becomes \"/components/schemas/User\"; node.at should be invoked with this decoded pointer and its result returned."
                }
            ]
        },
        {
            "group": "Null reference argument – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": null
                        }
                    ],
                    "expected": "Throws NullPointerException because the method dereferences the argument with startsWith().",
                    "description": "Passing a null reference should cause the first call to ref.startsWith(...) to throw a NullPointerException, which is the expected behavior for this edge case."
                }
            ]
        }
    ]
}