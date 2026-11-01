{
    "group": "Invalid URI handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new URI(\"http://example.com/invalid|char.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "IllegalArgumentException is thrown (thrown by SchemaLocation.of when the URI string contains characters that are not allowed in an IRI)",
            "description": "Pass a URI that contains characters illegal for an IRI (e.g., a pipe '|'). The method should forward the string to SchemaLocation.of, which validates the IRI and throws IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new URI(\"ftp://example.com/schema.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "IllegalArgumentException is thrown (unsupported URI scheme for a JSON Schema location)",
            "description": "Pass a URI with a scheme other than http, https, or file (e.g., ftp). SchemaLocation.of validates that the scheme is acceptable for schema locations and should reject unsupported schemes with IllegalArgumentException."
        }
    ]
}