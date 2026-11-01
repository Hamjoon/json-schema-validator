{
  "testCases": [
    {
      "group": "Version flag handling",
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
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri equals the IRI of the V4 meta‑schema and whose internal meta‑schema map contains exactly that meta‑schema.",
          "description": "When a supported version flag is supplied and no customizer is provided, getInstance should create a factory with the default meta‑schema derived from the version."
        },
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
          "expected": "Throws IllegalArgumentException (or NullPointerException) from JsonSchemaFactory.checkVersion because the version flag is null.",
          "description": "The method must validate the version flag; a null flag should cause an exception before any builder logic is executed."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.UNKNOWN"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "Throws IllegalArgumentException indicating the version flag is not supported.",
          "description": "If an unsupported enum constant is passed, checkVersion should reject it, exercising the error branch."
        }
      ]
    },
    {
      "group": "Customizer execution",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V7"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.defaultMetaSchemaIri(\"http://custom/meta\").enableSchemaCache(false)"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose defaultMetaSchemaIri is \"http://custom/meta\" and whose schema cache is disabled, while still containing the V7 meta‑schema added by the method before the customizer runs.",
          "description": "When a non‑null customizer is supplied it must be invoked after the factory has been pre‑populated with the version’s meta‑schema. The customizer can override properties (e.g., defaultMetaSchemaIri) and change other builder flags."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V201909"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.metaSchema(new JsonMetaSchema(\"http://example/custom\", ...))"
            }
          ],
          "expected": "Returns a JsonSchemaFactory whose internal meta‑schema map contains both the version‑derived meta‑schema and the additional custom meta‑schema added by the customizer.",
          "description": "The customizer may add extra meta‑schemas; the test verifies that the builder’s meta‑schema collection is correctly merged."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "builder -> { throw new RuntimeException(\"boom\"); }"
            }
          ],
          "expected": "Throws RuntimeException with message \"boom\"; the exception propagates out of getInstance.",
          "description": "If the customizer itself throws, getInstance must not swallow the exception – this exercises the branch where customizer.accept(...) propagates an error."
        }
      ]
    }
  ]
}