{
  "testCases": [
    {
      "group": "getInstance without customizer – null version flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": null
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance that uses the library's default SpecVersion (currently V7). No exception is thrown.",
          "description": "When the version flag is null the method should fall back to the default schema version and still return a usable factory."
        }
      ]
    },
    {
      "group": "getInstance without customizer – explicit version flags",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V4"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance configured for Draft‑04 (VersionFlag.V4). The factory should be able to load a Draft‑04 meta‑schema (e.g., getMetaSchema(\"http://json-schema.org/draft-04/schema#\") returns a non‑null JsonMetaSchema).",
          "description": "Pass a concrete version flag (V4) and verify that the returned factory is set up for that version."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V6"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance configured for Draft‑06 (VersionFlag.V6). The factory should be able to load a Draft‑06 meta‑schema (e.g., getMetaSchema(\"http://json-schema.org/draft-06/schema#\") returns a non‑null JsonMetaSchema).",
          "description": "Pass a concrete version flag (V6) and verify that the returned factory is set up for that version."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V7"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance configured for Draft‑07 (VersionFlag.V7). The factory should be able to load a Draft‑07 meta‑schema (e.g., getMetaSchema(\"http://json-schema.org/draft-07/schema#\") returns a non‑null JsonMetaSchema).",
          "description": "Pass a concrete version flag (V7) and verify that the returned factory is set up for that version."
        }
      ]
    },
    {
      "group": "getInstance with customizer – indirect branch verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V7"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance where a customizer supplied via the overloaded getInstance(VersionFlag, Consumer) has been applied. For example, if the customizer sets a custom URI loader, the resulting factory should use that loader (verified by calling factory.getSchemaLoader() and checking the custom behaviour).",
          "description": "Although the focal method itself does not receive a customizer, it delegates to the two‑argument overload. This test invokes the overload with a non‑null customizer to ensure the branch that applies the customizer is exercised."
        }
      ]
    }
  ]
}