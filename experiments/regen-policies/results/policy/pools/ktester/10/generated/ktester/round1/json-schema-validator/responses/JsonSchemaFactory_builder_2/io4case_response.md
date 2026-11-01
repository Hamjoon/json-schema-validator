{
    "tests": [
        {
            "group": "builder(JsonSchemaFactory) - null fields handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance where jsonMapper == null, yamlMapper == null, defaultMetaSchemaURI == null, and jsonMetaSchemas map is empty"
                        }
                    ],
                    "expected": "Returned Builder has jsonMapper == null, yamlMapper == null, defaultMetaSchemaURI == null, and its internal jsonMetaSchemas map is empty; schemaLoadersBuilder and schemaMappersBuilder remain null (as in blueprint).",
                    "description": "Ensures that the method correctly copies null values for optional fields and does not create accidental defaults."
                },
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance where jsonMapper is a custom ObjectMapper, yamlMapper is a custom ObjectMapper, defaultMetaSchemaURI is a non‑null string, and jsonMetaSchemas map is empty"
                        }
                    ],
                    "expected": "Returned Builder contains the same jsonMapper and yamlMapper instances, the same defaultMetaSchemaURI string, and an empty jsonMetaSchemas map; no loader or mapper builders are created.",
                    "description": "Verifies that non‑null mapper objects and default URI are copied verbatim even when there are no meta‑schemas."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - meta‑schema normalization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance with a single JsonMetaSchema whose URI is \"HTTP://example.com/SCHEMA/\" (mixed case, trailing slash)"
                        }
                    ],
                    "expected": "Returned Builder contains one meta‑schema entry whose key is the normalized URI \"http://example.com/schema\" (lower‑cased, no trailing slash) and the same JsonMetaSchema instance as the blueprint.",
                    "description": "Checks that the builder normalizes meta‑schema URIs using JsonSchemaFactory.normalizeMetaSchemaUri."
                },
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance with two JsonMetaSchema objects: one with URI \"http://example.com/schema\" and another with URI \"HTTP://EXAMPLE.COM/SCHEMA\" (different case, same logical URI)"
                        }
                    ],
                    "expected": "Returned Builder ends up with a single meta‑schema entry (the second one overwrites the first after normalization) keyed by \"http://example.com/schema\"; the stored JsonMetaSchema is the one that was added last.",
                    "description": "Ensures that duplicate URIs after normalization collapse to a single entry."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - builder independence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance with a non‑empty jsonMetaSchemas map (one entry) and a custom jsonMapper; after calling builder(blueprint) the test removes the entry from the returned Builder's jsonMetaSchemas map"
                        }
                    ],
                    "expected": "The blueprint's original jsonMetaSchemas map still contains the entry (unchanged), proving that the builder gets a copy, not a reference, of the map.",
                    "description": "Validates that modifications to the Builder's internal collections do not affect the original blueprint."
                },
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance where schemaLoadersBuilder is non‑null; after calling builder(blueprint) the test modifies the returned Builder's schemaLoadersBuilder (e.g., adds a new loader)."
                        }
                    ],
                    "expected": "The blueprint's schemaLoadersBuilder remains unchanged, confirming that the builder receives a cloned SchemaLoaders.Builder via SchemaLoaders.builder().with(...).",
                    "description": "Checks deep‑copy behaviour for nested builder objects."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - enableUriSchemaCache default",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "Blueprint instance whose internal Builder had enableUriSchemaCache set to false (via Builder.enableUriSchemaCache(false))"
                        }
                    ],
                    "expected": "Returned Builder has enableUriSchemaCache == true (the default), because builder(JsonSchemaFactory) does not copy this flag from the blueprint.",
                    "description": "Ensures that the enableUriSchemaCache flag is not propagated from the blueprint and defaults to true."
                }
            ]
        }
    ]
}