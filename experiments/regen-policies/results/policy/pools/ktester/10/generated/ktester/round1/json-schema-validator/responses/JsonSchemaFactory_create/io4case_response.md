{
    "groups": [
        {
            "group": "JsonSchemaFactory.create – null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) because schemaNode is required",
                    "description": "The factory must reject a null JsonNode representing the schema definition. This exercises the argument‑validation branch that is not covered by the existing tests."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – non‑null parentSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/parent.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"string\\\" }\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "parentSchema"
                        }
                    ],
                    "expected": "non‑null JsonSchema whose getParentSchema() returns the supplied parentSchema",
                    "description": "Pass an already created JsonSchema as the parent. The resulting schema should keep a reference to its parent, allowing inheritance of configuration (e.g., $id, $ref resolution)."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – non‑root evaluationPath",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.of(\"/properties/id\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"integer\\\" }\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema (no exception) – the factory must correctly handle a non‑root evaluationPath when creating a subschema",
                    "description": "When a subschema is created for a property, the evaluationPath points to that property. The method must accept any JsonNodePath, not only ROOT."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – boolean true schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "BooleanNode.TRUE"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema that always validates (no validators that can fail)",
                    "description": "A schema node that is the literal boolean true represents an always‑valid schema. The factory should create a JsonSchema that never produces validation errors."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – boolean false schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "BooleanNode.FALSE"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema that always fails validation (e.g., contains an AlwaysInvalidValidator)",
                    "description": "A literal false schema must produce a JsonSchema that rejects any instance. This tests the branch that handles boolean schema nodes."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – $schema version detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectMapper.readTree(\"{ \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema#\\\", \\\"type\\\": \\\"number\\\" }\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema whose meta‑schema version is Draft‑07 (JsonMetaSchema.getVersion() == SpecVersion.VersionFlag.V7)",
                    "description": "The presence of a $schema keyword should cause the factory to load the corresponding meta‑schema. This verifies that version detection works for a non‑default draft."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – allOf composite schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectMapper.readTree(\"{ \\\"allOf\\\": [ { \\\"type\\\": \\\"string\\\" }, { \\\"maxLength\\\": 5 } ] }\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema containing an AllOfValidator with two child schemas (string type + maxLength=5)",
                    "description": "The factory must recognise the allOf keyword, create sub‑schemas for each entry and combine them with an AllOfValidator."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.create – definitions without $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectMapper.readTree(\"{ \\\"definitions\\\": { \\\"positiveInteger\\\": { \\\"type\\\": \\\"integer\\\", \\\"minimum\\\": 0 } } }\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema that registers the definition (accessible via $ref) but does not create any top‑level validators",
                    "description": "Even when no $ref is used, definitions must be stored in the schema's internal registry so that later references can resolve. This case ensures that the factory processes the definitions node correctly."
                }
            ]
        }
    ]
}