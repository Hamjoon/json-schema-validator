{
  "groups": [
    {
      "group": "Different version flags – default configuration",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V6"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri equals the IRI of the V6 meta‑schema and whose internal meta‑schema map contains exactly that meta‑schema (size 1). The schema cache remains enabled (default).",
          "description": "Validate that getInstance correctly resolves the V6 version flag when no customizer is supplied."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V7"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri equals the IRI of the V7 meta‑schema and whose internal meta‑schema map contains exactly that meta‑schema (size 1).",
          "description": "Same as above but for the V7 flag – ensures each supported flag maps to its own meta‑schema."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V201909"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri equals the IRI of the V201909 meta‑schema and whose internal meta‑schema map contains exactly that meta‑schema (size 1).",
          "description": "Confirm handling of the 2019‑09 version flag without a customizer."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V202012"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri equals the IRI of the V202012 meta‑schema and whose internal meta‑schema map contains exactly that meta‑schema (size 1).",
          "description": "Confirm handling of the most recent version flag (2020‑12) without a customizer."
        }
      ]
    },
    {
      "group": "No‑op customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> { /* do nothing */ }"
            }
          ],
          "expected": "Returns a JsonSchemaFactory identical to the one produced when customizer is null: defaultMetaSchemaIri equals V4 meta‑schema IRI, internal meta‑schema map size 1, schema cache enabled.",
          "description": "A customizer that performs no operations must not alter the factory; this verifies that the method does not treat a non‑null but empty customizer specially."
        }
      ]
    },
    {
      "group": "Customizer modifies schema loaders",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.schemaLoaders(b -> b.addLoader(new SchemaLoader() { @Override public JsonSchema loadSchema(SchemaLocation location, SchemaValidatorsConfig cfg) { return null; } }))"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri is still the V4 IRI, schema cache enabled, and whose internal SchemaLoaders builder has been invoked (no exception thrown).",
          "description": "Ensures that the customizer can safely customize the SchemaLoaders component after the version meta‑schema has been pre‑populated."
        }
      ]
    },
    {
      "group": "Customizer adds multiple meta‑schemas via collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.metaSchemas(map -> { map.put(\"http://custom/one\", new JsonMetaSchema(\"http://custom/one\", JsonMetaSchema.getDefaultMetaSchema()) ); map.put(\"http://custom/two\", new JsonMetaSchema(\"http://custom/two\", JsonMetaSchema.getDefaultMetaSchema()) ); })"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose internal meta‑schema map contains three entries: the V4 meta‑schema plus the two custom meta‑schemas added by the customizer (size 3). The defaultMetaSchemaIri remains the V4 IRI.",
          "description": "Verifies that the builder’s metaSchemas(Consumer) API correctly merges additional meta‑schemas supplied by the customizer."
        }
      ]
    },
    {
      "group": "Customizer overrides object mappers",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.jsonMapper(new ObjectMapper())"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose getJsonMapper() returns the ObjectMapper instance supplied by the customizer (not the default JsonMapperFactory instance). The defaultMetaSchemaIri is still the V4 IRI.",
          "description": "Ensures that a custom jsonMapper supplied via the builder replaces the default mapper."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.yamlMapper(new ObjectMapper())"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose getYamlMapper() returns the ObjectMapper instance supplied by the customizer. The defaultMetaSchemaIri remains the V4 IRI.",
          "description": "Ensures that a custom yamlMapper supplied via the builder replaces the default yaml mapper."
        }
      ]
    },
    {
      "group": "Customizer sets defaultMetaSchemaIri to null",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.defaultMetaSchemaIri(null)"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose internal defaultMetaSchemaIri is null (overridden by the customizer) while still containing the V4 meta‑schema in its meta‑schema map. No exception is thrown.",
          "description": "Tests that the customizer can explicitly null‑out the defaultMetaSchemaIri, overriding the value set by getInstance before the customizer runs."
        }
      ]
    }
  ]
}