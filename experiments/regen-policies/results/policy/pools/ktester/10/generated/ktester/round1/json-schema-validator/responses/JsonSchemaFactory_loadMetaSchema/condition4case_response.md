{
    "testCases": [
        {
            "group": "Specification is null (no version information)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/meta-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonMetaSchema built with the given id and without any vocabularies. The builder’s `vocabularies` map remains null.",
                    "description": "The meta‑schema returned by `getSchema` has a ValidationContext whose MetaSchema’s `getSpecification()` returns null. The method should skip the version check and never touch the `$vocabulary` node, returning a JsonMetaSchema that only contains the id."
                }
            ]
        },
        {
            "group": "Specification older than V201909 (no vocabularies processing)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/old-meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonMetaSchema built with the given id and without any vocabularies. The builder’s `vocabularies` map remains null even if the schema node contains a `$vocabulary` field.",
                    "description": "The meta‑schema’s specification version is lower than V201909 (e.g., V201909.getVersionFlagValue() - 1). The method must not enter the vocabularies block, so any `$vocabulary` node present in the schema is ignored."
                }
            ]
        },
        {
            "group": "Specification newer than or equal to V201909 but `$vocabulary` node is absent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/new-meta-no-vocab\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonMetaSchema built with the given id and without any vocabularies. The builder’s `vocabularies` map remains null because the schema node does not contain a `$vocabulary` field.",
                    "description": "The meta‑schema’s specification is V201909 or newer, so the version check passes, but the schema node does not contain a `$vocabulary` property. The method should create the builder, skip the loop, and return a JsonMetaSchema with no vocabularies."
                }
            ]
        },
        {
            "group": "Specification newer than or equal to V201909 with `$vocabulary` node present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/new-meta-with-vocab\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonMetaSchema built with the given id and a vocabularies map containing all entries from the `$vocabulary` node. For example, if `$vocabulary` = {\"core\": true, \"applicator\": false}, the resulting JsonMetaSchema’s `vocabularies` map should be {\"core\": true, \"applicator\": false}.",
                    "description": "The meta‑schema’s specification is V201909 or newer and the schema node contains a `$vocabulary` object. The method must initialise the builder’s vocabularies map, iterate over each property, and store the boolean values. The final JsonMetaSchema should expose the same map."
                }
            ]
        }
    ]
}