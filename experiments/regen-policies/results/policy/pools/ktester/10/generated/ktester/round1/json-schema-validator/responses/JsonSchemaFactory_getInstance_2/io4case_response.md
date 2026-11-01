{
    "newTestCases": [
        {
            "group": "different supported versionFlag with null customizer (V6)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        },
                        {
                            "parameter": "customizer",
                            "value": "null"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "factory.defaultMetaSchemaURI equals the built‑in meta‑schema URI for V6",
                                "expression": "factory.getMetaSchema(factory.defaultMetaSchemaURI, new SchemaValidatorsConfig()).getUri().equals(JsonSchemaVersion.V6.getInstance().getUri())"
                            },
                            {
                                "description": "the built‑in V6 meta‑schema is registered in the factory’s internal map",
                                "expression": "factory.getMetaSchema(JsonSchemaVersion.V6.getInstance().getUri(), new SchemaValidatorsConfig()) != null"
                            }
                        ]
                    },
                    "description": "Verify that getInstance correctly initialises a factory for a supported version other than those already tested when no customizer is supplied."
                }
            ]
        },
        {
            "group": "customizer adds an additional meta‑schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.addMetaSchema(customMetaSchema)"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "the built‑in V6 meta‑schema is still registered",
                                "expression": "factory.getMetaSchema(JsonSchemaVersion.V6.getInstance().getUri(), new SchemaValidatorsConfig()) != null"
                            },
                            {
                                "description": "the custom meta‑schema supplied by the customizer is registered",
                                "expression": "factory.getMetaSchema(customMetaSchema.getUri(), new SchemaValidatorsConfig()) != null"
                            },
                            {
                                "description": "the internal meta‑schema map contains at least two entries (built‑in + custom)",
                                "expression": "factory.jsonMetaSchemas.size() >= 2"
                            }
                        ]
                    },
                    "description": "Ensure that a customizer can add extra JsonMetaSchema objects to the builder and that they are present in the resulting factory."
                }
            ]
        },
        {
            "group": "customizer overrides defaultMetaSchemaURI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.defaultMetaSchemaURI(\"http://example.com/custom-meta\")"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "factory.defaultMetaSchemaURI is replaced by the value supplied by the customizer",
                                "expression": "factory.defaultMetaSchemaURI.equals(\"http://example.com/custom-meta\")"
                            },
                            {
                                "description": "the built‑in V7 meta‑schema is still registered (it was added before the customizer runs)",
                                "expression": "factory.getMetaSchema(JsonSchemaVersion.V7.getInstance().getUri(), new SchemaValidatorsConfig()) != null"
                            }
                        ]
                    },
                    "description": "Check that a customizer can change the defaultMetaSchemaURI after the builder has been pre‑populated with the version‑specific meta‑schema."
                }
            ]
        },
        {
            "group": "customizer toggles enableUriSchemaCache flag twice",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.enableUriSchemaCache(false).enableUriSchemaCache(true)"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "final value of enableUriSchemaCache reflects the last call (true)",
                                "expression": "factory.enableUriSchemaCache == true"
                            },
                            {
                                "description": "defaultMetaSchemaURI still matches the V7 meta‑schema URI",
                                "expression": "factory.getMetaSchema(factory.defaultMetaSchemaURI, new SchemaValidatorsConfig()).getUri().equals(JsonSchemaVersion.V7.getInstance().getUri())"
                            }
                        ]
                    },
                    "description": "Validate that multiple invocations on the same builder property are applied in order and the last one wins."
                }
            ]
        },
        {
            "group": "customizer adds a collection of meta‑schemas",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.addMetaSchemas(java.util.Arrays.asList(customMeta1, customMeta2))"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "both custom meta‑schemas are registered",
                                "expression": "factory.getMetaSchema(customMeta1.getUri(), new SchemaValidatorsConfig()) != null && factory.getMetaSchema(customMeta2.getUri(), new SchemaValidatorsConfig()) != null"
                            },
                            {
                                "description": "the built‑in V6 meta‑schema is also present",
                                "expression": "factory.getMetaSchema(JsonSchemaVersion.V6.getInstance().getUri(), new SchemaValidatorsConfig()) != null"
                            },
                            {
                                "description": "total number of registered meta‑schemas is at least three",
                                "expression": "factory.jsonMetaSchemas.size() >= 3"
                            }
                        ]
                    },
                    "description": "Confirm that the builder’s addMetaSchemas(Collection) method works when invoked from a customizer."
                }
            ]
        },
        {
            "group": "customizer configures schemaLoaders and schemaMappers",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V4"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.schemaLoaders(b -> b.addSchemaLoader(customLoader)).schemaMappers(b -> b.addSchemaMapper(customMapper))"
                        }
                    ],
                    "expected": {
                        "type": "instance",
                        "class": "JsonSchemaFactory",
                        "assertions": [
                            {
                                "description": "the custom SchemaLoader is present in the factory’s loader chain",
                                "expression": "factory.getSchemaLoader().getLoaders().contains(customLoader)"
                            },
                            {
                                "description": "the custom SchemaMapper is present in the factory’s mapper chain",
                                "expression": "factory.getSchemaMappers().getMappers().contains(customMapper)"
                            }
                        ]
                    },
                    "description": "Ensure that a customizer can customise the SchemaLoaders and SchemaMappers builders and that those custom components are reflected in the resulting factory."
                }
            ]
        }
    ]
}