{
  "group": "builder(JsonSchemaFactory) - handling of empty blueprint",
  "cases": [
    {
      "input": [
        {
          "parameter": "blueprint",
          "value": "JsonSchemaFactory created via JsonSchemaFactory.builder().build() (all defaults, no meta‑schemas, no URI mappings, no custom factories/fetchers, no translators)"
        }
      ],
      "expected": "Returns a Builder whose internal state mirrors the blueprint: \n- addMetaSchemas receives an empty collection, so the builder’s jsonMetaSchemas map is empty.\n- defaultMetaSchemaURI is null (same as blueprint).\n- objectMapper and yamlMapper are the same instances as in the blueprint.\n- addUriMappings receives an empty map, so the builder’s uriMap is empty.\n- No URITranslator is added because blueprint.uriTranslators is empty.\n- No custom URIFactory or URIFetcher entries are added because the blueprint’s factories/fetchers maps are empty.\nVerification can be done by calling builder.build() and asserting that the resulting JsonSchemaFactory has the same defaultMetaSchemaURI, the same ObjectMapper/YAMLMapper instances, and that its URIFactory, URIFetcher and URITranslator contain no entries beyond the built‑in defaults.",
      "description": "Ensures the method correctly copies an empty/default blueprint without throwing and without adding any extra entries."
    }
  ]
}