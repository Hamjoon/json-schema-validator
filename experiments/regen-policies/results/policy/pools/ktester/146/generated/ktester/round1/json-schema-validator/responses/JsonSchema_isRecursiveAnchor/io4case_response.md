{
    "testCases": [
        {
            "group": "JsonSchema.isRecursiveAnchor() - true flag in schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": true }"
                        }
                    ],
                    "expected": true,
                    "description": "When the schema contains the \"$recursiveAnchor\": true keyword, the created JsonSchema instance must report true via isRecursiveAnchor()."
                }
            ]
        },
        {
            "group": "JsonSchema.isRecursiveAnchor() - explicit false flag in schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": false }"
                        }
                    ],
                    "expected": false,
                    "description": "When the schema explicitly sets \"$recursiveAnchor\": false, the instance should return false, confirming that the flag is respected even when present."
                }
            ]
        },
        {
            "group": "JsonSchema.isRecursiveAnchor() - non‑boolean value for $recursiveAnchor",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": \"yes\" }"
                        }
                    ],
                    "expected": false,
                    "description": "If the \"$recursiveAnchor\" keyword is present but not a boolean (e.g., a string), the parser should ignore it and the schema should behave as if the flag were absent, resulting in false."
                }
            ]
        },
        {
            "group": "JsonSchema.isRecursiveAnchor() - parent and child schemas independent flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentSchemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": true }"
                        },
                        {
                            "parameter": "childSchemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": false, \"allOf\": [{ \"$ref\": \"#\" }] }"
                        }
                    ],
                    "expected": {
                        "parent": true,
                        "child": false
                    },
                    "description": "Create a parent schema with \"$recursiveAnchor\": true and a child schema (referencing the parent via $ref) with \"$recursiveAnchor\": false. Verify that each JsonSchema instance reports its own flag correctly, demonstrating that the flag is not inherited."
                }
            ]
        },
        {
            "group": "JsonSchema.isRecursiveAnchor() - flag after schema cloning via withConfig",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"$recursiveAnchor\": true }"
                        },
                        {
                            "parameter": "configModification",
                            "value": "none"
                        }
                    ],
                    "expected": true,
                    "description": "Obtain a JsonSchema instance with \"$recursiveAnchor\": true, then call withConfig(...) (even if the config does not change the flag). The returned schema should preserve the original recursiveAnchor value."
                }
            ]
        }
    ]
}