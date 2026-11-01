{
  "testCases": [
    {
      "group": "customizer is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V4"
            },
            {
              "parameter": "customizer",
              "value": "null"
            }
          ],
          "expected": {
            "type": "instance",
            "class": "JsonSchemaFactory",
            "assertions": [
              {
                "description": "factory.defaultMetaSchemaURI equals the URI of the built‑in meta‑schema for the supplied version",
                "expression": "factory.getMetaSchema(factory.defaultMetaSchemaURI, new SchemaValidatorsConfig()).getUri().equals(JsonSchemaVersion.V4.getInstance().getUri())"
              },
              {
                "description": "the built‑in meta‑schema is registered in the factory’s internal map",
                "expression": "factory.getMetaSchema(JsonSchemaVersion.V4.getInstance().getUri(), new SchemaValidatorsConfig()) != null"
              }
            ]
          },
          "description": "Verify that getInstance returns a factory with the correct default meta‑schema when no customizer is supplied."
        }
      ]
    },
    {
      "group": "customizer modifies builder",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.V7"
            },
            {
              "parameter": "customizer",
              "value": "builder -> builder.enableUriSchemaCache(false)"
            }
          ],
          "expected": {
            "type": "instance",
            "class": "JsonSchemaFactory",
            "assertions": [
              {
                "description": "customizer is executed – the builder’s enableUriSchemaCache flag is set to false",
                "expression": "factory.enableUriSchemaCache == false"
              },
              {
                "description": "defaultMetaSchemaURI still matches the version’s meta‑schema URI",
                "expression": "factory.getMetaSchema(factory.defaultMetaSchemaURI, new SchemaValidatorsConfig()).getUri().equals(JsonSchemaVersion.V7.getInstance().getUri())"
              }
            ]
          },
          "description": "Ensure that a non‑null customizer is invoked and can alter the builder state (e.g., disabling the URI schema cache)."
        }
      ]
    },
    {
      "group": "null versionFlag",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "null"
            },
            {
              "parameter": "customizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getInstance with a null VersionFlag should throw NullPointerException (the method forwards the argument to checkVersion which dereferences it)."
        }
      ]
    },
    {
      "group": "unsupported versionFlag (future enum value)",
      "cases": [
        {
          "input": [
            {
              "parameter": "versionFlag",
              "value": "SpecVersion.VersionFlag.UNKNOWN"
            },
            {
              "parameter": "customizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If an enum constant that is not mapped to a JsonSchemaVersion is supplied, checkVersion should throw IllegalArgumentException."
        }
      ]
    }
  ]
}