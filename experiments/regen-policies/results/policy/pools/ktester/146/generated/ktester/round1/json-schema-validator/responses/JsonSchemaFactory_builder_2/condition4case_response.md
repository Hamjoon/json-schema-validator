{
  "group": "builder(JsonSchemaFactory) – blueprint with null optional builders",
  "cases": [
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "JsonSchemaFactory instance where schemaLoadersBuilder == null and schemaMappersBuilder == null, metaSchemas contains a single entry {\"http://example.com/meta\": dummyMetaSchema}, defaultMetaSchemaIri = \"http://example.com/meta\", jsonMapper = customJsonMapper, yamlMapper = customYamlMapper, jsonNodeReader = customReader"
        }
      ],
      "expected": "Returned Builder has: schemaLoadersBuilder == null, schemaMappersBuilder == null, metaSchemas map contains the same entry with normalized IRI, defaultMetaSchemaIri equal to blueprint's, jsonMapper, yamlMapper and jsonNodeReader equal to those supplied in the blueprint. No NullPointerException is thrown.",
      "description": "Verifies that when the blueprint does not define schemaLoadersBuilder or schemaMappersBuilder the builder copies all other fields correctly and leaves those two fields null."
    }
  ]
}