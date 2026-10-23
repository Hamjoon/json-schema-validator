{
  "groups": [
    {
      "group": "Factory instance uniqueness",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            }
          ],
          "expected": "Two distinct JsonSchemaFactory objects are returned on successive calls (different object references)",
          "description": "Calling getInstance with the same supported flag twice must produce independent factory instances; the method should not cache or reuse a singleton."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V2020_12"
            }
          ],
          "expected": "Two distinct JsonSchemaFactory objects are returned on successive calls (different object references)",
          "description": "Even for the latest draft, getInstance must create a fresh factory each invocation."
        }
      ]
    },
    {
      "group": "Meta‑schema registration count and key normalization",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V6"
            }
          ],
          "expected": "Factory's internal jsonMetaSchemas map has size 1 and contains the normalized key \"http://json-schema.org/draft-06/schema#\"",
          "description": "After getInstance(V6) the factory should register only the Draft‑06 meta‑schema; the key must be the normalized URI returned by JsonMetaSchema.getUri()."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V2019_09"
            }
          ],
          "expected": "Factory's internal jsonMetaSchemas map has size 1 and contains the normalized key \"https://json-schema.org/draft/2019-09/schema#\"",
          "description": "For the 2019‑09 draft the factory must contain exactly one meta‑schema entry with the correct HTTPS URI."
        }
      ]
    },
    {
      "group": "Builder defaults are not altered by getInstance",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V7"
            }
          ],
          "expected": "Returned JsonSchemaFactory has enableUriSchemaCache == true and its uriTranslators collection is empty (default state)",
          "description": "getInstance should build the factory using the Builder's default configuration; no custom URI translators or cache settings are introduced automatically."
        }
      ]
    }
  ]
}