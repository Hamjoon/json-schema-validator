{
    "test_cases": [
        {
            "group": "Known $schema with null config",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema corresponds to Draft‑04 (the meta‑schema identified by the $schema URI) and whose config reference is null (or a default‑created SchemaValidatorsConfig)",
                    "description": "When a recognised $schema URI is present but the caller supplies a null configuration, the factory must still resolve the correct meta‑schema and handle the null config gracefully."
                }
            ]
        },
        {
            "group": "Empty $schema value falls back to default",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema (e.g., Draft‑7) and config is the supplied instance",
                    "description": "An empty string for the $schema attribute should be treated as absent; the method must therefore fall back to the default meta‑schema."
                }
            ]
        },
        {
            "group": "Whitespace‑padded $schema URI treated as unknown",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"   http://json-schema.org/draft-04/schema#   \",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema because the whitespace‑padded URI is not recognised",
                    "description": "The implementation does not trim the $schema value; therefore a URI with leading/trailing spaces should be considered unknown and trigger the fallback logic."
                }
            ]
        },
        {
            "group": "$schema attribute with explicit null value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":null,\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema (fallback) and config is the supplied instance",
                    "description": "A JSON null for $schema should be ignored, causing the factory to use the default meta‑schema."
                }
            ]
        },
        {
            "group": "Configuration with multiple flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(true).setTypeLoose(true).setFailFast(true).build()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema and whose config reflects validateAgainstMetaSchema=true, typeLoose=true, failFast=true",
                    "description": "Ensures that a fully‑populated SchemaValidatorsConfig is passed unchanged into the created ValidationContext."
                }
            ]
        },
        {
            "group": "Recognised newer draft (2020‑12) via $schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema corresponds to Draft‑2020‑12 (the meta‑schema identified by the $schema URI)",
                    "description": "Verifies that the factory can resolve a newer, supported draft URI and create a ValidationContext with that meta‑schema."
                }
            ]
        },
        {
            "group": "$schema URI containing fragment identifier",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"http://json-schema.org/draft-04/schema#/definitions/foo\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema corresponds to Draft‑04 (the fragment part is ignored for meta‑schema resolution)",
                    "description": "The fragment identifier after the meta‑schema URI should not affect the lookup; the factory must still resolve to Draft‑04."
                }
            ]
        }
    ]
}