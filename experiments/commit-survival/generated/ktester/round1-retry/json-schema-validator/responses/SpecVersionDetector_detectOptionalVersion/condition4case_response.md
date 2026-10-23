{
  "group": "detectOptionalVersion",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{}"
        }
      ],
      "expected": "Optional.empty",
      "description": "When the JSON object does not contain the \"$schema\" property, the method should return an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_07)",
      "description": "A recognizable schema URI is provided. After normalization the URI maps to VersionFlag.DRAFT_07, so the method returns an Optional containing that flag."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"http://example.com/unknown-schema\" }"
        }
      ],
      "expected": "JsonSchemaException: \"'http://example.com/unknown-schema' is unrecognizable schema\"",
      "description": "The \"$schema\" value is not known to SpecVersion.VersionFlag. The method should throw a JsonSchemaException with a message indicating the schema is unrecognizable."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"$schema\": \"draft-07\" }"
        }
      ],
      "expected": "Optional.of(VersionFlag.DRAFT_07)",
      "description": "A short form schema identifier that requires normalization. JsonSchemaFactory.normalizeMetaSchemaUri converts \"draft-07\" to the full URI, which is then resolved to VersionFlag.DRAFT_07."
    }
  ]
}