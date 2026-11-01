{
    "testCases": [
        {
            "group": "Original schema recursiveAnchor true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "somePath"
                        }
                    ],
                    "expected": "Returned JsonSchema has recursiveAnchor == true because the flag is taken from the *original* schema (the one on which fromRef is invoked), not from the parent. All other fields are copied from the parent as usual.",
                    "description": "Verify that when the original schema has recursiveAnchor set to true, the new schema preserves that value, confirming the flag is sourced from the current instance."
                }
            ]
        },
        {
            "group": "Original schema with non‑default suppressSubSchemaRetrieval",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema has suppressSubSchemaRetrieval == true (matching the original schema). The flag is not taken from the parent schema.",
                    "description": "Ensure that the suppressSubSchemaRetrieval flag is copied from the invoking schema, not from the parent, and that the flag survives the constructor call."
                }
            ]
        },
        {
            "group": "Original schema id null propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.id == null (the original schema’s id was null). No NullPointerException is thrown.",
                    "description": "Validate that a null id on the original schema is propagated unchanged to the new instance."
                }
            ]
        },
        {
            "group": "Original schema with custom schemaNode reference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.schemaNode == originalSchemaMock.schemaNode (the same object reference).",
                    "description": "Confirm that the schemaNode field is not cloned but directly passed to the new JsonSchema, preserving reference equality."
                }
            ]
        },
        {
            "group": "Original schema with non‑null errorMessage fields",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.errorMessage, errorMessageKeyword, errorMessageType, messageSource, keyword are identical to those of the original schema (copied by reference).",
                    "description": "Check that all error‑message related fields are transferred from the invoking schema to the new schema."
                }
            ]
        },
        {
            "group": "EvaluationParentSchema reference correctness",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "differentParentMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "somePath"
                        }
                    ],
                    "expected": "Returned JsonSchema.evaluationParentSchema == differentParentMock (exact same object). The new schema’s parentSchema field remains the original schema’s parent, not the ref‑evaluation parent.",
                    "description": "Validate that the constructor stores the supplied refEvaluationParentSchema as evaluationParentSchema while leaving the logical parentSchema unchanged."
                }
            ]
        },
        {
            "group": "EvaluationPath empty JsonNodePath",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.empty()"
                        }
                    ],
                    "expected": "Returned JsonSchema.evaluationPath is an empty JsonNodePath instance (not null). No exception is thrown.",
                    "description": "Ensure that an empty (but non‑null) JsonNodePath is accepted and stored correctly."
                }
            ]
        },
        {
            "group": "Parent schema with null config object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullConfigMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.getValidationContext().getConfig() == null. The method does not throw an exception despite the parent’s config being null.",
                    "description": "Confirm that a null config in the parent’s ValidationContext is tolerated and propagated to the new ValidationContext."
                }
            ]
        },
        {
            "group": "Parent schema with non‑empty dynamicAnchors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithDynamicAnchorsMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.getValidationContext().getDynamicAnchors() contains the same entries as the parent’s dynamicAnchors map.",
                    "description": "Verify that dynamicAnchors are copied verbatim from the parent schema’s ValidationContext."
                }
            ]
        },
        {
            "group": "Parent schema with distinct meta‑schema and factory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithCustomMetaAndFactoryMock"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "pathMock"
                        }
                    ],
                    "expected": "Returned JsonSchema.getValidationContext().getMetaSchema() == parentSchemaWithCustomMetaAndFactoryMock.validationContext.getMetaSchema() AND getJsonSchemaFactory() == parentSchemaWithCustomMetaAndFactoryMock.validationContext.getJsonSchemaFactory().",
                    "description": "Ensure that both meta‑schema and jsonSchemaFactory are copied from the parent’s ValidationContext."
                }
            ]
        }
    ]
}