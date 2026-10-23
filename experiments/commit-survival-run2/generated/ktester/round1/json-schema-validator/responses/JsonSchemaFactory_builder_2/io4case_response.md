{
  "testCases": [
    {
      "group": "builder(JsonSchemaFactory) - copies defaultMetaSchemaURI",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().defaultMetaSchemaURI(\"https://example.com/meta\").build()"
            }
          ],
          "expected": "The factory returned by builder(blueprint).build() has its defaultMetaSchemaURI equal to \"https://example.com/meta\" (identical to the blueprint).",
          "description": "Verifies that a non‑null defaultMetaSchemaURI set on the blueprint is propagated to the new factory."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies custom ObjectMapper and YAMLMapper instances",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().objectMapper(customObjectMapper).yamlMapper(customYamlMapper).build()"
            }
          ],
          "expected": "The resulting factory uses the same ObjectMapper instance (customObjectMapper) and the same YAMLMapper instance (customYamlMapper) as the blueprint.",
          "description": "Ensures that custom mapper objects are not replaced by defaults when cloning via the builder method."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies added meta‑schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().addMetaSchema(meta1).addMetaSchema(meta2).build() where meta1.getUri() = \"urn:meta:1\" and meta2.getUri() = \"urn:meta:2\""
            }
          ],
          "expected": "The new factory contains both meta‑schemas with normalized URIs (\"urn:meta:1\" and \"urn:meta:2\") in its jsonMetaSchemas map, matching the blueprint’s collection.",
          "description": "Checks that all meta‑schemas added to the blueprint are duplicated in the cloned factory."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies deprecated URI mappings (uriMap)",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().addUriMappings(Map.of(\"http://old.com\", \"http://new.com\")).build()"
            }
          ],
          "expected": "The cloned factory’s internal uriMap contains the entry \"http://old.com\" → \"http://new.com\" exactly as in the blueprint.",
          "description": "Even though addUriMappings is deprecated, the builder method must copy the map from the blueprint."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies custom URIFactory mappings",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().uriFactory(customFactory, \"custom\").build()"
            }
          ],
          "expected": "The resulting factory’s URIFactory contains a mapping for scheme \"custom\" that resolves to the same customFactory instance.",
          "description": "Validates that custom scheme‑to‑factory associations are transferred to the new factory."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies custom URIFetcher mappings",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().uriFetcher(customFetcher, \"custom\").build()"
            }
          ],
          "expected": "The new factory’s URIFetcher holds a mapping for scheme \"custom\" that points to the same customFetcher instance.",
          "description": "Ensures that custom scheme‑to‑fetcher associations are preserved when cloning."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - copies URITranslators",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().addUriTranslator(translatorA).addUriTranslator(translatorB).build()"
            }
          ],
          "expected": "The cloned factory’s CompositeURITranslator contains both translatorA and translatorB in the same order.",
          "description": "Checks that all URITranslator objects added to the blueprint are present in the new factory."
        }
      ]
    },
    {
      "group": "builder(JsonSchemaFactory) - does NOT copy urnFactory or enableUriSchemaCache",
      "cases": [
        {
          "input": [
            {
              "parameter": "blueprint",
              "value": "JsonSchemaFactory built with Builder().addUrnFactory(customUrnFactory).enableUriSchemaCache(false).build()"
            }
          ],
          "expected": "The factory produced by builder(blueprint).build() has urnFactory == null (default) and its internal enableUriSchemaCache flag remains true (the builder’s default).",
          "description": "The builder method intentionally ignores the blueprint’s urnFactory and enableUriSchemaCache settings; this test confirms that the defaults are used instead of the blueprint’s values."
        }
      ]
    }
  ]
}