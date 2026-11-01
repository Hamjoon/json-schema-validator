{
    "test_cases": [
        {
            "group": "builder(JsonSchemaFactory) – null blueprint",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling JsonSchemaFactory.builder(null) should throw a NullPointerException because the method dereferences the blueprint to copy its fields."
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
                            "value": "JsonSchemaFactory instance where metaSchemas == null, all other fields set to valid non‑null values (e.g., defaultMetaSchemaIri = \"http://example.com/meta\", jsonMapper = new ObjectMapper())"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The builder method calls blueprint.metaSchemas.values(); if metaSchemas is null a NullPointerException must be thrown."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) – blueprint with metaSchemas containing a null entry",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance where metaSchemas map contains a single entry with key \"http://example.com/meta\" and value null, other fields (defaultMetaSchemaIri, jsonMapper, yamlMapper, jsonNodeReader) are valid"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "During builder creation the method iterates over blueprint.metaSchemas.values() and calls metaSchema(jsonMetaSchema) for each element. A null element causes a NullPointerException when metaSchema() attempts to access jsonMetaSchema.getIri()."
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
                            "value": "JsonSchemaFactory instance where metaSchemas map contains an entry whose JsonMetaSchema.getIri() returns null, other fields are valid"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "normalizeMetaSchemaUri(String id) is invoked with the IRI returned by jsonMetaSchema.getIri(). If that IRI is null the method is expected to throw a NullPointerException."
                }
            ]
        }
    ]
}