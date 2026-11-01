{
    "test_cases": [
        {
            "group": "builder(JsonSchemaFactory) - null blueprint",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling builder(null) should throw NPE because the method dereferences blueprint immediately (jsonMetaSchemas, defaultMetaSchemaURI, etc.)."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - null internal meta‑schema map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance whose jsonMetaSchemas field is null (constructed via reflection or a custom subclass)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method calls blueprint.jsonMetaSchemas.values(); if the map itself is null a NPE is thrown."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - null meta‑schema entry",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory with jsonMetaSchemas containing a null value (e.g., map.put(\"http://example.com\", null))"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "During addMetaSchemas the Builder iterates over the collection and passes a null JsonMetaSchema to addMetaSchema, which dereferences jsonMetaSchema.getUri() and throws NPE."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - meta‑schema with null URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory with a JsonMetaSchema whose getUri() returns null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "addMetaSchema calls normalizeMetaSchemaUri(jsonMetaSchema.getUri()); a null URI causes normalizeMetaSchemaUri to throw NPE."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - schemaLoadersBuilder with null inside with()",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory where schemaLoadersBuilder is non‑null but its internal state makes SchemaLoaders.builder().with(blueprint.schemaLoadersBuilder) throw IllegalArgumentException (e.g., builder was built with an illegal configuration)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the original SchemaLoaders.Builder cannot be cloned via with(...), the with method is expected to raise an IllegalArgumentException, which propagates from builder(JsonSchemaFactory)."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - schemaMappersBuilder with null inside with()",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory where schemaMappersBuilder is non‑null but its with(...) method throws IllegalStateException due to invalid internal configuration"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "When cloning a non‑null SchemaMappers.Builder, an IllegalStateException may be thrown if the builder's state is inconsistent; this exception should surface from builder(JsonSchemaFactory)."
                }
            ]
        }
    ]
}