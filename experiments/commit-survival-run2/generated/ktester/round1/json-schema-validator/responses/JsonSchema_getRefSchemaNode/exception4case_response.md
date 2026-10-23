{
    "test_cases": [
        {
            "group": "findAncestor returns null – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#/properties/name"
                        }
                    ],
                    "expected": "Throws NullPointerException because findAncestor() returns null and the subsequent call to schema.getSchemaNode() dereferences a null reference.",
                    "description": "The method starts by calling findAncestor(). If the mocked JsonSchema instance is set up so that findAncestor() returns null, the next line `schema.getSchemaNode()` will cause a NullPointerException before any other logic (prefix checks, URL decoding, etc.) is executed."
                }
            ]
        },
        {
            "group": "schema.getSchemaNode returns null – NullPointerException on node.at",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#/missingNode"
                        }
                    ],
                    "expected": "Throws NullPointerException because schema.getSchemaNode() returns null and the method attempts to invoke `node.at(jsonPointer)` on a null JsonNode.",
                    "description": "The reference begins with \"#/\" so the method will strip the leading \"#\" and enter the JSON‑Pointer branch. The node variable is obtained from `schema.getSchemaNode()`. If that call is mocked to return null, the subsequent `node.at(jsonPointer)` dereferences a null object, producing a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "/directPointer"
                        }
                    ],
                    "expected": "Throws NullPointerException because schema.getSchemaNode() returns null and the method attempts to invoke `node.at(jsonPointer)` on a null JsonNode.",
                    "description": "A reference that starts directly with \"/\" also triggers the JSON‑Pointer branch. With `schema.getSchemaNode()` mocked to null, the call to `node.at(jsonPointer)` results in a NullPointerException."
                }
            ]
        },
        {
            "group": "metaSchema is null – NullPointerException on fragment resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "#fragment"
                        }
                    ],
                    "expected": "Throws NullPointerException because the JsonSchema instance's metaSchema field is null and the method attempts to call `metaSchema.getNodeByFragmentRef(...)`.",
                    "description": "The reference starts with \"#\" and has length > 1, so the method enters the fragment‑reference branch. If the `metaSchema` dependency is mocked (or left unset) to be null, the call `this.metaSchema.getNodeByFragmentRef(ref, node)` dereferences a null reference, causing a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "ref",
                            "value": "urn:example:schema"
                        }
                    ],
                    "expected": "Throws NullPointerException because the JsonSchema instance's metaSchema field is null and the method attempts to call `metaSchema.getNodeByFragmentRef(...)`.",
                    "description": "The reference starts with \"urn:\" and length > 4, so the same fragment‑reference branch is taken. With `metaSchema` null, the method throws a NullPointerException when invoking `getNodeByFragmentRef`."
                }
            ]
        }
    ]
}