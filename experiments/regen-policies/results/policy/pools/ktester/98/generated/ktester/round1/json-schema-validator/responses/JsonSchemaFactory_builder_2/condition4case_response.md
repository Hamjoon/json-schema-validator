{
  "testCases": [
    {
      "group": "builder(JsonSchemaFactory) – both internal builders are null",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory created via Builder with metaSchemas empty, defaultMetaSchemaIri = \"http://example.com/meta\", jsonMapper = new ObjectMapper(), yamlMapper = new ObjectMapper(), jsonNodeReader = null, schemaLoadersBuilder = null, schemaMappersBuilder = null"
            }
          ],
          "expected": "Returned Builder has metaSchemas equal to blueprint.metaSchemas, defaultMetaSchemaIri equal to \"http://example.com/meta\", jsonMapper and yamlMapper equal to the ones supplied, jsonNodeReader == null, and both schemaLoadersBuilder and schemaMappersBuilder remain null.",
          "description": "When the blueprint does not contain custom SchemaLoaders or SchemaMappers, the static builder method should copy the simple fields and leave the internal builder references untouched (null)."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – only schemaLoadersBuilder is non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory created via Builder with a non‑null schemaLoadersBuilder (configured with a dummy loader) and null schemaMappersBuilder"
            }
          ],
          "expected": "Returned Builder has the same metaSchemas, defaultMetaSchemaIri, jsonMapper, yamlMapper and jsonNodeReader as the blueprint; schemaLoadersBuilder is a new SchemaLoaders.Builder that contains the blueprint's loader (i.e., not null); schemaMappersBuilder remains null.",
          "description": "The first if‑branch should be taken, copying the blueprint's SchemaLoaders builder via SchemaLoaders.builder().with(...). The second if‑branch must be skipped because schemaMappersBuilder is null."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – only schemaMappersBuilder is non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory created via Builder with a non‑null schemaMappersBuilder (configured with a dummy mapper) and null schemaLoadersBuilder"
            }
          ],
          "expected": "Returned Builder copies all simple fields; schemaMappersBuilder is a new SchemaMappers.Builder that contains the blueprint's mapper (i.e., not null); schemaLoadersBuilder remains null.",
          "description": "The second if‑branch should be executed while the first is skipped, ensuring only the SchemaMappers builder is transferred."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) – both internal builders are non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory created via Builder with both schemaLoadersBuilder and schemaMappersBuilder set (each configured with a dummy implementation)"
            }
          ],
          "expected": "Returned Builder copies all simple fields; both schemaLoadersBuilder and schemaMappersBuilder are non‑null and correspond to new builders created by SchemaLoaders.builder().with(blueprint.schemaLoadersBuilder) and SchemaMappers.builder().with(blueprint.schemaMappersBuilder).",
          "description": "Both if‑branches should be taken, confirming that the method correctly clones both internal builders from the blueprint."
        }
      ]
    }
  ]
}