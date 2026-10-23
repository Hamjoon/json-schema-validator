{
  "testCases": [
    {
      "group": "Valid version flags - each supported Draft",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            }
          ],
          "expected": "JsonSchemaFactory instance with defaultMetaSchemaURI == \"http://json-schema.org/draft-04/schema#\" and contains the V4 meta‑schema",
          "description": "When a supported Draft‑04 flag is supplied, getInstance should return a non‑null factory whose default meta‑schema URI matches the V4 meta‑schema URI."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V6"
            }
          ],
          "expected": "JsonSchemaFactory instance with defaultMetaSchemaURI == \"http://json-schema.org/draft-06/schema#\" and contains the V6 meta‑schema",
          "description": "When a supported Draft‑06 flag is supplied, getInstance should return a factory configured for Draft‑06."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V7"
            }
          ],
          "expected": "JsonSchemaFactory instance with defaultMetaSchemaURI == \"http://json-schema.org/draft-07/schema#\" and contains the V7 meta‑schema",
          "description": "When a supported Draft‑07 flag is supplied, getInstance should return a factory configured for Draft‑07."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V2019_09"
            }
          ],
          "expected": "JsonSchemaFactory instance with defaultMetaSchemaURI == \"https://json-schema.org/draft/2019-09/schema#\" and contains the 2019‑09 meta‑schema",
          "description": "When the 2019‑09 flag is supplied, getInstance should return a factory configured for the 2019‑09 draft."
        },
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V2020_12"
            }
          ],
          "expected": "JsonSchemaFactory instance with defaultMetaSchemaURI == \"https://json-schema.org/draft/2020-12/schema#\" and contains the 2020‑12 meta‑schema",
          "description": "When the 2020‑12 flag is supplied, getInstance should return a factory configured for the 2020‑12 draft."
        }
      ]
    },
    {
      "group": "Invalid version flag - null",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) thrown by checkVersion",
          "description": "Passing a null flag should cause the method to abort with an exception because checkVersion cannot resolve a null version."
        }
      ]
    },
    {
      "group": "Invalid version flag - unsupported enum constant",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.UNSUPPORTED"
            }
          ],
          "expected": "IllegalArgumentException thrown by checkVersion indicating unsupported version",
          "description": "If an enum constant that is not mapped to any JsonSchemaVersion is supplied, checkVersion must reject it and getInstance should propagate the exception."
        }
      ]
    }
  ]
}