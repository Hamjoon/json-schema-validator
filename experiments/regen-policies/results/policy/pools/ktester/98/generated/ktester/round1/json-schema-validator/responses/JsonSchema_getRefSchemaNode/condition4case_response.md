{
    "group": "getRefSchemaNode",
    "cases": [
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "simpleRef"
                }
            ],
            "expected": "original schema node",
            "description": "Schema has no id (getId() returns null) and the ref does not start with \"#/\" nor \"/\". None of the conditional branches are taken, so the method should return the root schema node unchanged."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "http://example.com/schemas/main#/defs/thing"
                }
            ],
            "expected": "node at path \"/defs/thing\"",
            "description": "Schema has id \"http://example.com/schemas/main\". The ref starts with this id, so the first if strips the id leaving \"#/defs/thing\". The second if removes the leading \"#\", yielding \"/defs/thing\". The third if is true, the pointer is decoded (no change) and node.at(\"/defs/thing\") returns an existing sub‑node. The method should return that sub‑node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "http://example.com/schemas/main/defs/thing"
                }
            ],
            "expected": "node at path \"/defs/thing\"",
            "description": "Schema id matches the beginning of the ref, stripping it leaves \"/defs/thing\" (no leading \"#\"). The second if is skipped, the third if processes the pointer, decodes it and node.at(\"/defs/thing\") returns an existing sub‑node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#/components/schemas/Person"
                }
            ],
            "expected": "node at path \"/components/schemas/Person\"",
            "description": "Schema has no id. The ref starts with \"#/\", so the second if removes the leading \"#\" producing \"/components/schemas/Person\". The third if then resolves the pointer and returns the corresponding sub‑node."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/missing/path"
                }
            ],
            "expected": "node returned by handleNullNode",
            "description": "Schema has no id and the ref starts directly with \"/\". After decoding, node.at(\"/missing/path\") yields a MissingNode, causing the method to call handleNullNode(ref, schema) and return its result."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "http://example.com/schemas/main"
                }
            ],
            "expected": "original schema node",
            "description": "Ref is exactly the schema id. After the first if the jsonPointer becomes an empty string. Neither the \"#/\" nor the \"/\" checks succeed, so the original schema node is returned."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "#plainFragment"
                }
            ],
            "expected": "original schema node",
            "description": "Ref starts with \"#\" but not with \"#/\". The second if is false, the third if is also false, therefore the method returns the original schema node."
        }
    ]
}