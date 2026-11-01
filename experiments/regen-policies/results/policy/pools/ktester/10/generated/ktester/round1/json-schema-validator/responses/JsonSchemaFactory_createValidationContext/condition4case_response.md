{
    "testCases": [
        {
            "group": "Default meta‑schema selection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema (e.g., Draft‑7) and config is the supplied instance",
                    "description": "When the schema JSON does not contain a \"$schema\" attribute, the factory should fall back to its default meta‑schema. This verifies the branch where getMetaSchemaOrDefault returns the default."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema and config is null (or the factory creates a default config internally)",
                    "description": "Ensures that a null SchemaValidatorsConfig does not break the method and the default meta‑schema is still selected."
                }
            ]
        },
        {
            "group": "Custom meta‑schema via $schema attribute",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema corresponds to Draft‑04 (the meta‑schema identified by the $schema URI)",
                    "description": "When the schema JSON contains a known \"$schema\" URI, getMetaSchemaOrDefault should resolve and return the matching meta‑schema. This covers the branch that looks up a meta‑schema by ID."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"http://unknown/schema#\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is the factory's default meta‑schema (fallback because the $schema URI is not recognised)",
                    "description": "When the \"$schema\" attribute points to an unknown or unmapped URI, the factory should fall back to the default meta‑schema. This exercises the fallback branch inside getMetaSchemaOrDefault."
                }
            ]
        },
        {
            "group": "Null schemaNode handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) thrown by createValidationContext",
                    "description": "The method expects a non‑null JsonNode; passing null should trigger an exception, covering the defensive‑programming branch."
                }
            ]
        },
        {
            "group": "Custom configuration influencing meta‑schema selection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(false).build()"
                        }
                    ],
                    "expected": "ValidationContext whose JsonMetaSchema is still the default meta‑schema, but the config flag validateAgainstMetaSchema is false",
                    "description": "Even when validation against the meta‑schema is disabled via config, the factory still creates a ValidationContext; this verifies that config is passed unchanged to the context."
                }
            ]
        }
    ]
}