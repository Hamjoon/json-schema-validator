{
    "testCases": [
        {
            "group": "Well‑known dialect handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://json-schema.org/draft-07/schema#\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "A JsonMetaSchema instance that equals JsonSchemaVersion.DRAFT_07.getInstance() (i.e., the meta‑schema for Draft‑07). No call to loadMetaSchema should occur.",
                    "description": "The supplied id is a recognised JSON‑Schema dialect. The method should detect the version, map it through checkVersion, obtain the corresponding JsonSchemaVersion instance and return its JsonMetaSchema. This covers the true‑branch of the Optional.map chain."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"https://json-schema.org/draft/2020-12/schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "A JsonMetaSchema instance that equals JsonSchemaVersion.DRAFT_2020_12.getInstance().",
                    "description": "Another well‑known dialect (2020‑12). Verifies that the method works for any dialect that SpecVersionDetector can resolve, not just Draft‑07."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://json-schema.org/draft-04/schema#\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "A JsonMetaSchema instance that equals JsonSchemaVersion.DRAFT_04.getInstance().",
                    "description": "Ensures older dialects are also correctly resolved via the Optional path."
                }
            ]
        },
        {
            "group": "Custom meta‑schema handling (fallback branch)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"urn:custom:my-meta-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "The JsonMetaSchema returned by JsonSchemaFactory.loadMetaSchema(\"urn:custom:my-meta-schema\", config). The test should verify that loadMetaSchema is invoked exactly once and that its result is returned.",
                    "description": "The id is not recognised by SpecVersionDetector, so the Optional is empty and the lambda supplied to orElseGet is executed. This covers the fallback branch."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "The JsonMetaSchema returned by loadMetaSchema(null, config). The method should not throw NPE; it should delegate to loadMetaSchema.",
                    "description": "Edge case where the id argument is null. The detection step should return Optional.empty, causing the fallback to be taken. Verifies robust handling of null ids."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "The JsonMetaSchema returned by loadMetaSchema(\"\", config).",
                    "description": "Empty string id, also not a known dialect, should trigger the fallback branch."
                }
            ]
        }
    ]
}