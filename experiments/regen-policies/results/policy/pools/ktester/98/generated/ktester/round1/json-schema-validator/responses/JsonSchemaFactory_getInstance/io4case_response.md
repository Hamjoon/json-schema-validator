{
  "tests": [
    {
      "group": "getInstance without customizer – newer draft versions",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V2019_09"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance configured for Draft‑2019‑09 (VersionFlag.V2019_09). Calling factory.getMetaSchema(\"https://json-schema.org/draft/2019-09/schema#\") returns a non‑null JsonMetaSchema whose version matches V2019_09.",
          "description": "Verify that the factory correctly recognises the 2019‑09 draft flag and can load its meta‑schema."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V2020_12"
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance configured for Draft‑2020‑12 (VersionFlag.V2020_12). Calling factory.getMetaSchema(\"https://json-schema.org/draft/2020-12/schema#\") returns a non‑null JsonMetaSchema whose version matches V2020_12.",
          "description": "Verify that the factory correctly recognises the 2020‑12 draft flag and can load its meta‑schema."
        }
      ]
    },
    {
      "group": "getInstance caching behavior – same version returns same instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V6"
            }
          ],
          "expected": "Two consecutive calls to JsonSchemaFactory.getInstance(VersionFlag.V6) return the *exact same* object reference (instance). No new objects are created, confirming internal caching.",
          "description": "Ensures that the factory caches instances per version flag and re‑uses them."
        }
      ]
    },
    {
      "group": "getInstance caching behavior – different versions return different instances",
      "cases": [
        {
          "input": [
            {
              "parameter": "firstVersionFlag",
              "value": "V4"
            },
            {
              "parameter": "secondVersionFlag",
              "value": "V7"
            }
          ],
          "expected": "The objects returned by JsonSchemaFactory.getInstance(V4) and JsonSchemaFactory.getInstance(V7) are not the same reference (they are distinct instances).",
          "description": "Confirms that caching is scoped to the version flag and does not mistakenly return a single shared instance for all versions."
        }
      ]
    },
    {
      "group": "getInstance overload with null customizer – indirect branch verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "V7"
            },
            {
              "parameter": "customizer",
              "value": null
            }
          ],
          "expected": "A non‑null JsonSchemaFactory instance identical to the one returned by the single‑argument overload. No customisation occurs and no exception is thrown.",
          "description": "Calls the two‑argument overload with a null Consumer to verify that the method correctly forwards to the single‑argument path without applying any customisation."
        }
      ]
    }
  ]
}