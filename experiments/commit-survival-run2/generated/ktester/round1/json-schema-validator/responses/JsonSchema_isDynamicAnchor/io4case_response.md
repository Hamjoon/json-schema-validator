{
    "groups": [
        {
            "group": "isDynamicAnchor_fromSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{ \"$dynamicAnchor\": true }"
                        }
                    ],
                    "expected": "true",
                    "description": "When a JsonSchema is created via the static factory `JsonSchema.from` with a schema node that contains the `$dynamicAnchor` keyword set to true, the internal `dynamicAnchor` flag should be set and `isDynamicAnchor()` must return true."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{ }"
                        }
                    ],
                    "expected": "false",
                    "description": "When a JsonSchema is created via `JsonSchema.from` with a schema node that does **not** contain the `$dynamicAnchor` keyword, the flag remains at its default value and `isDynamicAnchor()` must return false."
                }
            ]
        },
        {
            "group": "isDynamicAnchor_childSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentDynamicAnchor",
                            "value": "true"
                        },
                        {
                            "parameter": "childSchemaPath",
                            "value": "\"#/properties/name\""
                        },
                        {
                            "parameter": "childSchemaNode",
                            "value": "{ \"type\": \"string\" }"
                        }
                    ],
                    "expected": "false",
                    "description": "A child schema created with `createChildSchema` should **not** inherit the parent’s dynamic‑anchor flag. Even if the parent schema’s `dynamicAnchor` field is true (set via reflection or factory), the newly created child schema must have its own flag initialized to false, so `isDynamicAnchor()` on the child returns false."
                }
            ]
        }
    ]
}