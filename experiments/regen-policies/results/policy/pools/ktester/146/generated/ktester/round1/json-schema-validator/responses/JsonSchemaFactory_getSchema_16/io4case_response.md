{
    "group": "Fragment URI handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new URI(\"http://example.com/schema.json#section1\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"id\\\":{\\\"type\\\":\\\"string\\\"}}}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchema instance whose getSchemaLocation().toString() equals \"http://example.com/schema.json#section1\" and whose getSchemaNode() matches the supplied JsonNode",
            "description": "Verify that a URI containing a fragment identifier is preserved in the resulting SchemaLocation while the supplied JsonNode is used as the schema definition."
        }
    ]
  ,
  "group": "Relative URI handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new URI(\"relative/path/to/schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"integer\\\"}}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchema instance whose getSchemaLocation().toString() equals \"relative/path/to/schema.json\" and whose schema node equals the supplied JsonNode",
      "description": "Ensure that a relative URI (no scheme) is accepted and stored verbatim in the SchemaLocation; the factory must not attempt to resolve it when a JsonNode is already supplied."
    }
  ]
  ,
  "group": "Empty schema node",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new URI(\"http://example.com/empty.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new ObjectMapper().readTree(\"{}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchema instance whose getSchemaNode() is an empty ObjectNode and whose location string matches the supplied URI",
      "description": "Check that the factory can create a JsonSchema even when the supplied JsonNode represents an empty schema (no \"type\" keyword)."
    }
  ]
  ,
  "group": "Custom config propagation",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new URI(\"http://example.com/custom-config.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig(){ { setFailFast(true); } }"
        }
      ],
      "expected": "JsonSchema instance whose internal SchemaValidatorsConfig has failFast set to true (i.e., config is propagated unchanged)",
      "description": "Validate that a non‑default SchemaValidatorsConfig supplied to getSchema is retained by the created JsonSchema, allowing custom validation behaviour."
    }
  ]
  ,
  "group": "Encoded URI handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new URI(\"http://example.com/schema%20with%20spaces.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchema instance whose getSchemaLocation().toString() equals \"http://example.com/schema%20with%20spaces.json\" and whose schema node matches the supplied JsonNode",
      "description": "Confirm that URIs containing percent‑encoded characters are accepted and stored exactly as provided, without triggering a URISyntaxException."
    }
  ]
}