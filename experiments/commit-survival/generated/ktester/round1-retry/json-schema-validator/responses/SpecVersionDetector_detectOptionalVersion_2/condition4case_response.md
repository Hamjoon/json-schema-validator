{
  "group": "SpecVersionDetector.detectOptionalVersion(String)",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "http://json-schema.org/draft-04/schema#"
        }
      ],
      "expected": "Optional.of(VersionFlag.V4)",
      "description": "A known Draft‑04 schema URI should be resolved to VersionFlag.V4 wrapped in an Optional."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "http://json-schema.org/draft-06/schema#"
        }
      ],
      "expected": "Optional.of(VersionFlag.V6)",
      "description": "A known Draft‑06 schema URI should be resolved to VersionFlag.V6 wrapped in an Optional."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "http://json-schema.org/draft-07/schema#"
        }
      ],
      "expected": "Optional.of(VersionFlag.V7)",
      "description": "A known Draft‑07 schema URI should be resolved to VersionFlag.V7 wrapped in an Optional."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "http://example.com/unknown-schema"
        }
      ],
      "expected": "Optional.empty()",
      "description": "An unknown schema URI should not match any VersionFlag and therefore return an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": null
        }
      ],
      "expected": "Optional.empty()",
      "description": "A null argument should be handled gracefully and result in an empty Optional (no exception is thrown)."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": ""
        }
      ],
      "expected": "Optional.empty()",
      "description": "An empty string does not correspond to any known version and should return an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "   http://json-schema.org/draft-04/schema#   "
        }
      ],
      "expected": "Optional.empty()",
      "description": "If the method does not trim whitespace, a URI with leading/trailing spaces should be considered unknown and return an empty Optional."
    }
  ]
}