{
    "group": "getRefSchemaNode",
    "cases": [
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#/"
                }
            ],
            "expected": "original schema node",
            "description": "Ref is exactly \"#/\". After the second if the pointer becomes \"/\"; the third if processes it, node.at(\"/\") returns the root node, which is the original schema node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/components/schemas/Person"
                }
            ],
            "expected": "node at path \"/components/schemas/Person\"",
            "description": "Ref starts with a leading \"/\" (no \"#\"). The method decodes (no change) and resolves the JSON Pointer to an existing sub‑node. The original schema has an id, but it does not match the ref, so the id‑stripping branch is skipped."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#/defs%2Fthing"
                }
            ],
            "expected": "node at path \"/defs/thing\"",
            "description": "Ref contains an URL‑encoded slash. After the second if the pointer is \"/defs%2Fthing\"; the third if decodes it to \"/defs/thing\" and node.at resolves to the expected sub‑node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/defs%2Fthing"
                }
            ],
            "expected": "node at path \"/defs/thing\"",
            "description": "Ref starts with \"/\" and includes an encoded slash. The pointer is decoded to \"/defs/thing\" and the method returns the corresponding node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": ""
                }
            ],
            "expected": "original schema node",
            "description": "Empty string ref does not trigger any conditional branch; the method returns the root schema node unchanged."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#"
                }
            ],
            "expected": "original schema node",
            "description": "Ref is a solitary \"#\" (no following \"/\"). Neither the second nor the third if conditions are satisfied, so the original schema node is returned."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": null
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null reference causes a NullPointerException when the method evaluates ref.startsWith(...)."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "http://example.com/schemas/mainExtra#/defs/thing"
                }
            ],
            "expected": "original schema node",
            "description": "Schema id is \"http://example.com/schemas/main\". The ref starts with this id, so the id is stripped leaving \"Extra#/defs/thing\". Because the remaining pointer does not start with \"#\" or \"/\", no further processing occurs and the original schema node is returned."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#/missing/node"
                }
            ],
            "expected": "node returned by handleNullNode",
            "description": "Ref starts with \"#/\"; after stripping it becomes \"/missing/node\". node.at(\"/missing/node\") yields a MissingNode, causing the method to invoke handleNullNode(ref, schema) and return its result."
        }
    ]
}