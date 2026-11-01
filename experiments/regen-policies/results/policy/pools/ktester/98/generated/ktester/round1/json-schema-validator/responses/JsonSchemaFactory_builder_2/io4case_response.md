{
    "tests": [
        {
            "group": "builder(JsonSchemaFactory) – copy of non‑empty metaSchemas and immutability",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with two JsonMetaSchema objects (iri \"http://example.com/meta1\" and \"http://example.com/meta2\"), defaultMetaSchemaIri = \"http://example.com/default\", jsonMapper = new ObjectMapper(), yamlMapper = new ObjectMapper(), jsonNodeReader = null, schemaLoadersBuilder = null, schemaMappersBuilder = null"
                        }
                    ],
                    "expected": "Returned Builder contains a metaSchemas map with two entries whose keys are the normalized IRIs of the supplied meta‑schemas (same as the original IRIs) and the same JsonMetaSchema instances; modifications to the Builder's metaSchemas (e.g., removing an entry) do not affect the blueprint's metaSchemas collection.",
                    "description": "Verify that a non‑empty metaSchemas collection is copied correctly and that the copy is independent of the original blueprint."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – null defaultMetaSchemaIri handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with defaultMetaSchemaIri explicitly set to null, all other fields populated with non‑null values"
                        }
                    ],
                    "expected": "Returned Builder has defaultMetaSchemaIri == null while all other simple fields (jsonMapper, yamlMapper, jsonNodeReader, metaSchemas) are copied from the blueprint.",
                    "description": "Ensure that a null defaultMetaSchemaIri is preserved in the new Builder rather than being replaced by a default value."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – copy of jsonNodeReader when present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with jsonNodeReader = JsonNodeReader.builder().locationAware().build(), jsonMapper = new ObjectMapper(), yamlMapper = new ObjectMapper(), defaultMetaSchemaIri = \"http://example.com/meta\""
                        }
                    ],
                    "expected": "Returned Builder has jsonNodeReader equal to the instance supplied in the blueprint; jsonMapper and yamlMapper are also copied but are ignored for reading because jsonNodeReader takes precedence.",
                    "description": "Confirm that when a JsonNodeReader is set on the blueprint it is copied verbatim to the new Builder."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – copy of yamlMapper when jsonMapper is null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with jsonMapper = null, yamlMapper = new ObjectMapper(), jsonNodeReader = null, defaultMetaSchemaIri = \"http://example.com/meta\""
                        }
                    ],
                    "expected": "Returned Builder has yamlMapper equal to the supplied ObjectMapper and jsonMapper == null.",
                    "description": "Validate that a null jsonMapper does not prevent the yamlMapper from being copied."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – metaSchema IRI normalization during copy",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder with a JsonMetaSchema whose iri is \"http://example.com/meta/\" (trailing slash). The Builder's metaSchemas map therefore stores the normalized key \"http://example.com/meta\"."
                        }
                    ],
                    "expected": "Returned Builder's metaSchemas map contains a single entry keyed by the normalized IRI \"http://example.com/meta\" and the associated JsonMetaSchema instance, demonstrating that normalizeMetaSchemaUri is applied during copying.",
                    "description": "Check that the static builder method normalizes meta‑schema IRIs when copying the metaSchemas collection."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – both internal builders non‑null with custom configuration",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory built via Builder where schemaLoadersBuilder has a custom loader added via .addLoader(\"custom\", (uri) -> new DefaultSchemaLoader()), and schemaMappersBuilder has a custom mapper added via .addMapper(\"custom\", (node) -> node); all other fields are default."
                        }
                    ],
                    "expected": "Returned Builder has schemaLoadersBuilder != null containing the custom loader named \"custom\" and schemaMappersBuilder != null containing the custom mapper named \"custom\"; both builders are new instances (different identity) but replicate the custom configurations from the blueprint.",
                    "description": "Ensure that when both internal builders are present, their custom configurations are cloned correctly, not just the builder objects."
                }
            ]
        }
    ]
}