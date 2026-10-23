{
  "group": "detectOptionalVersion",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_04)",
      "description": "A full meta‑schema URI for Draft‑04 is supplied; after normalization it maps to VersionFlag.DRAFT_04."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"draft-06\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_06)",
      "description": "A short‑form identifier for Draft‑06 is provided; normalizeMetaSchemaUri expands it to the known URI and the method returns the corresponding flag."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_2019_09)",
      "description": "The 2019‑09 meta‑schema URI is recognized and resolved to VersionFlag.DRAFT_2019_09."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_2020_12)",
      "description": "The 2020‑12 meta‑schema URI is recognized and resolved to VersionFlag.DRAFT_2020_12."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"\" }"
        }
      ],
      "expected": "JsonSchemaException: \"'' is unrecognizable schema\"",
      "description": "An empty string is supplied for $schema; after normalization the identifier is still empty, causing an exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": 123 }"
        }
      ],
      "expected": "JsonSchemaException: \"'123' is unrecognizable schema\"",
      "description": "A numeric value is supplied for $schema; asText() converts it to \"123\", which is not a known schema URI, leading to an exception."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": null }"
        }
      ],
      "expected": "JsonSchemaException: \"'null' is unrecognizable schema\"",
      "description": "Explicit null is provided for $schema; NullNode.asText() yields \"null\", which is not a recognized schema, so an exception is thrown."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": null
        }
      ],
      "expected": "NullPointerException",
      "description": "The method is invoked with a null JsonNode argument; a NullPointerException is expected because the implementation dereferences the argument without a null check."
    }
  ]
}