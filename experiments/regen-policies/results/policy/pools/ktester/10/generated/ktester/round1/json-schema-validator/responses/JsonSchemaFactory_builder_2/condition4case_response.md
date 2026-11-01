{
  "group": "builder(JsonSchemaFactory) - conditional branches",
  "cases": [
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "Blueprint instance where schemaLoadersBuilder == null and schemaMappersBuilder == null"
        }
      ],
      "expected": "Returned Builder has schemaLoadersBuilder == null and schemaMappersBuilder == null; all meta‑schemas from the blueprint are present; defaultMetaSchemaURI, jsonMapper and yamlMapper are copied unchanged.",
      "description": "Both conditional branches are false – verifies that the method does not create any loader or mapper builders when the blueprint does not provide them."
    },
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "Blueprint instance with a non‑null SchemaLoaders.Builder (custom loader) and schemaMappersBuilder == null"
        }
      ],
      "expected": "Returned Builder has a non‑null schemaLoadersBuilder that is a copy of the blueprint's loader builder; schemaMappersBuilder remains null; other fields are copied as in the blueprint.",
      "description": "First if‑branch true, second false – checks that the loader builder is cloned while the mapper builder is left untouched."
    },
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "Blueprint instance with schemaLoadersBuilder == null and a non‑null SchemaMappers.Builder (custom mapper)"
        }
      ],
      "expected": "Returned Builder has a non‑null schemaMappersBuilder that is a copy of the blueprint's mapper builder; schemaLoadersBuilder remains null; other fields are copied as in the blueprint.",
      "description": "First if‑branch false, second true – verifies that the mapper builder is cloned while the loader builder is not created."
    },
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "Blueprint instance with both schemaLoadersBuilder and schemaMappersBuilder non‑null"
        }
      ],
      "expected": "Returned Builder has non‑null schemaLoadersBuilder and schemaMappersBuilder, each being a copy of the corresponding builder from the blueprint; all other fields (meta‑schemas, default URI, mappers) are identical to the blueprint.",
      "description": "Both conditional branches true – ensures that the method copies both loader and mapper builders correctly."
    }
  ]
}