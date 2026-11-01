{
    "test_cases": [
        {
            "group": "builder(JsonSchemaFactory) – null blueprint parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null blueprint should cause a NullPointerException because the method dereferences the argument immediately."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with null metaSchemas map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory created via Builder with metaSchemas = null, other fields non‑null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the blueprint's internal metaSchemas map is null, the call to blueprint.metaSchemas.values() throws a NullPointerException."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with metaSchemas containing null entry",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder where metaSchemas map contains a null JsonMetaSchema value"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "During builder().metaSchemas(blueprint.metaSchemas.values()) the iteration reaches a null element, causing a NullPointerException when the Builder's metaSchema(JsonMetaSchema) method tries to call jsonMetaSchema.getIri()."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with metaSchema having null IRI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with a JsonMetaSchema whose getIri() returns null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Builder copies meta‑schemas by normalizing the IRI; a null IRI leads to a NullPointerException inside normalizeMetaSchemaUri."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with faulty schemaLoadersBuilder",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with a custom SchemaLoaders.Builder subclass that overrides with(...) to throw IllegalArgumentException"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the blueprint's schemaLoadersBuilder is non‑null, the method invokes SchemaLoaders.builder().with(blueprint.schemaLoadersBuilder). If the overridden with method throws, the exception propagates."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with faulty schemaMappersBuilder",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with a custom SchemaMappers.Builder subclass that overrides with(...) to throw IllegalStateException"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "Analogous to the previous case, but for the schemaMappersBuilder branch; an exception thrown by the overridden with method should be propagated."
                }
            ]
        }
    ]
}