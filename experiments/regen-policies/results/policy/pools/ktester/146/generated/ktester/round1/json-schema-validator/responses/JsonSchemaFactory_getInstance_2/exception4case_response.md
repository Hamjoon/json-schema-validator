{
  "test_cases": [
    {
      "group": "Customizer passes null to builder methods",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.metaSchema(null)"
            }
          ],
          "expected": "Throws NullPointerException because builder.metaSchema(null) attempts to call getIri() on a null JsonMetaSchema.",
          "description": "When the customizer supplies a null JsonMetaSchema to the builder, the builder's metaSchema method dereferences the argument, causing a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.metaSchemas((java.util.Collection)null)"
            }
          ],
          "expected": "Throws NullPointerException when builder.metaSchemas attempts to iterate over a null collection.",
          "description": "Passing a null collection to the metaSchemas(Collection) overload should result in a NullPointerException because the method iterates over the collection without a null‑check."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.schemaLoaders(null)"
            }
          ],
          "expected": "Throws NullPointerException because the schemaLoaders method invokes accept on a null Consumer.",
          "description": "The builder.schemaLoaders method expects a non‑null Consumer; supplying null leads to a NullPointerException when accept is called."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.schemaMappers(null)"
            }
          ],
          "expected": "Throws NullPointerException because the schemaMappers method invokes accept on a null Consumer.",
          "description": "Analogous to schemaLoaders, passing a null Consumer to schemaMappers results in a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.metaSchemas(null)"
            }
          ],
          "expected": "Throws NullPointerException because the metaSchemas(Consumer) method attempts to invoke accept on a null Consumer.",
          "description": "When the customizer supplies a null Consumer to the metaSchemas(Consumer) overload, the builder calls accept on it, causing a NullPointerException."
        }
      ]
    }
  ]
}