{
    "testCases": [
        {
            "group": "Successful navigation – object property segment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/properties/name\""
                        }
                    ],
                    "expected": "JsonSchema instance whose schema node is the object under \"properties\" → \"name\". The returned schema's getSchemaLocation().getFragment() should be \"#/properties/name\" and evaluationPath should be \"#/properties/name\".",
                    "description": "The fragment points to a child property of an object. The segment is a String and parent node is an ObjectNode, therefore the branch handling object property segments is exercised. A sub‑schema is created for the property and returned."
                }
            ]
        },
        {
            "group": "Successful navigation – $id resolution before final segment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/definitions/address/properties/street\""
                        }
                    ],
                    "expected": "JsonSchema instance representing the schema node at \"/definitions/address/properties/street\". Because the intermediate node \"/definitions/address\" contains an \"$id\" (e.g., \"http://example.com/address\"), a new sub‑schema is created at that point (x == 1) and the later segment \"street\" is resolved relative to the new lexical context. The final schemaLocation fragment should be \"#/definitions/address/properties/street\".",
                    "description": "The fragment traverses through an object that defines a new $id. The code path where `id != null` triggers creation of a new sub‑schema before reaching the last segment, testing the lexical context handling."
                }
            ]
        },
        {
            "group": "Segment is numeric but parent node is NOT an array",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/properties/0\""
                        }
                    ],
                    "expected": "JsonSchema instance for the property named \"0\" under \"properties\". The numeric segment is treated as a String because parentNode.isArray() is false, so the `else` branch is taken. The schemaLocation fragment should be \"#/properties/0\".",
                    "description": "Tests the case where the segment is a Number object but the current parent node is an ObjectNode. The method must not use the array‑index handling branch."
                }
            ]
        },
        {
            "group": "Fragment not found – fallback to external schema resource returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/nonexistent/path\""
                        }
                    ],
                    "expected": "InvalidSchemaRefException thrown with ValidationMessage type REF and code \"internal.unresolvedRef\". The instanceLocation in the message should match the current schemaLocation fragment at the point of failure.",
                    "description": "The fragment cannot be resolved in the current document. `getNode` returns null, triggering the fallback logic. The document's `findSchemaResourceRoot().fetchSubSchemaNode` also returns null, so the method throws an InvalidSchemaRefException. This covers the error‑handling branch."
                }
            ]
        },
        {
            "group": "Fragment not found – external schema resource resolves successfully",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/external/ref\""
                        }
                    ],
                    "expected": "JsonSchema instance obtained from the external schema resource. Its schemaLocation fragment should reflect the resolved external pointer (e.g., \"#/external/ref\").",
                    "description": "When the fragment is not present in the current document, `fetchSubSchemaNode` returns a non‑null JsonSchema that itself can resolve the fragment. The method returns the result of `found.getSubSchema(fragment)`. This tests the successful fallback path."
                }
            ]
        },
        {
            "group": "Last segment without $id – sub‑schema created only at final step",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/properties/age\""
                        }
                    ],
                    "expected": "JsonSchema instance for the \"age\" property. Since the intermediate node \"/properties\" has no $id and is not the last segment, no sub‑schema is created at that step; a sub‑schema is created only when x == nameCount‑1. The returned schemaLocation fragment should be \"#/properties/age\".",
                    "description": "Ensures that when `id` is null for an intermediate segment, the condition `(id != null) || (x == nameCount-1)` prevents premature sub‑schema creation, and the sub‑schema is only instantiated at the final segment."
                }
            ]
        }
    ]
}