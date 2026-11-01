{
  "testCases": [
    {
      "group": "builder(JsonSchemaFactory) – blueprint with non‑null schemaLoadersBuilder and schemaMappersBuilder",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory instance where schemaLoadersBuilder != null (configured with a custom ClasspathSchemaLoader), schemaMappersBuilder != null (configured with a custom SchemaMapper), metaSchemas contains one entry {\"http://example.com/meta\": dummyMetaSchema}, defaultMetaSchemaIri = \"http://example.com/meta\", jsonMapper = customJsonMapper, yamlMapper = customYamlMapper, jsonNodeReader = customReader"
            }
          ],
          "expected": "Returned Builder has a non‑null schemaLoadersBuilder that is a new instance (not the same object as blueprint.schemaLoadersBuilder) and contains the same custom configuration; similarly schemaMappersBuilder is a new instance with the same custom configuration. All other fields (metaSchemas map, defaultMetaSchemaIri, jsonMapper, yamlMapper, jsonNodeReader) are copied exactly. No NullPointerException is thrown.",
          "description": "Ensures that when the blueprint supplies schemaLoadersBuilder and schemaMappersBuilder, the factory builder creates deep copies of those builders while preserving their configuration."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – blueprint with empty metaSchemas and null defaultMetaSchemaIri",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory instance where metaSchemas is an empty ConcurrentHashMap, defaultMetaSchemaIri = null, jsonMapper = null, yamlMapper = null, jsonNodeReader = null, schemaLoadersBuilder = null, schemaMappersBuilder = null"
            }
          ],
          "expected": "Returned Builder has metaSchemas map empty (size 0), defaultMetaSchemaIri remains null, and the three mapper/reader fields stay null. schemaLoadersBuilder and schemaMappersBuilder remain null. The builder can be built without throwing an exception.",
          "description": "Verifies that the method correctly handles a blueprint that does not define any meta‑schemas nor a default IRI and that null mapper/reader fields are propagated."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – blueprint with multiple metaSchemas requiring IRI normalization",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory instance where metaSchemas contains two entries: {\"http://example.com/meta#\": metaSchemaA, \"http://example.com/meta\": metaSchemaB}, defaultMetaSchemaIri = \"http://example.com/meta#\", jsonMapper = customJsonMapper, yamlMapper = customYamlMapper, jsonNodeReader = customReader, schemaLoadersBuilder = null, schemaMappersBuilder = null"
            }
          ],
          "expected": "Returned Builder’s metaSchemas map contains a single entry with normalized IRI \"http://example.com/meta\" mapping to the last supplied JsonMetaSchema (metaSchemaB). The duplicate key (with '#') is overwritten after normalization. defaultMetaSchemaIri is normalized to \"http://example.com/meta\". All other fields are copied unchanged. No exception is thrown.",
          "description": "Checks that the builder normalizes IRIs via JsonSchemaFactory.normalizeMetaSchemaUri and that duplicate normalized keys are handled correctly."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – blueprint with null mapper/reader fields",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory instance where jsonMapper = null, yamlMapper = null, jsonNodeReader = null, metaSchemas contains one entry {\"http://example.com/meta\": dummyMetaSchema}, defaultMetaSchemaIri = \"http://example.com/meta\", schemaLoadersBuilder = null, schemaMappersBuilder = null"
            }
          ],
          "expected": "Returned Builder has jsonMapper, yamlMapper and jsonNodeReader all set to null. The metaSchemas map and defaultMetaSchemaIri are copied correctly. No NullPointerException occurs during builder creation or later build().",
          "description": "Ensures that the method tolerates null values for the deprecated mapper fields and the jsonNodeReader."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – defensive copy of metaSchemas map",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory instance where metaSchemas contains entry {\"http://example.com/meta\": dummyMetaSchema}, defaultMetaSchemaIri = \"http://example.com/meta\", all other fields null"
            }
          ],
          "expected": "After calling JsonSchemaFactory.builder(blueprint) the returned Builder contains its own ConcurrentMap instance. Mutating the Builder’s metaSchemas (e.g., adding a new entry) does NOT affect blueprint.metaSchemas. Conversely, modifying blueprint.metaSchemas after the builder is created does NOT affect the Builder’s map. No exception is thrown.",
          "description": "Validates that the builder method creates a defensive copy of the meta‑schemas map rather than sharing the original reference."
        }
      ]
    }
  ]
}