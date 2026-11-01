{
  "testCases": [
    {
      "group": "customizer null",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance whose defaultMetaSchemaIri equals the IRI of the JsonMetaSchema returned for version V4 (e.g. \"http://json-schema.org/draft-04/schema#\"). No customisation should be applied.",
          "description": "When the customiser argument is null, getInstance should create a builder, set the default meta‑schema IRI and meta‑schema from the supplied version flag, skip the customiser step and return the built factory."
        }
      ]
    },
    {
      "group": "customizer non‑null",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.defaultMetaSchemaIri(\"customIri\").metaSchema(customMetaSchema)"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance whose defaultMetaSchemaIri is \"customIri\" and which contains the supplied customMetaSchema (identified by its IRI). The customiser must have been invoked exactly once.",
          "description": "A non‑null customiser should be called with the builder, allowing the caller to override the default meta‑schema IRI and add additional meta‑schemas before the factory is built."
        }
      ]
    },
    {
      "group": "invalid version flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": null
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) thrown by JsonSchemaFactory.checkVersion because the version flag is null.",
          "description": "Passing a null version flag must cause the method to fail early via checkVersion, ensuring that no factory is created."
        }
      ]
    }
  ]
}