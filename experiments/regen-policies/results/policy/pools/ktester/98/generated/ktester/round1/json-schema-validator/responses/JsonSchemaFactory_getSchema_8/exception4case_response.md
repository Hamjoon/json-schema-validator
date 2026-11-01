{
    "group": "JsonSchemaFactory.getSchema(JsonNode, SchemaValidatorsConfig) - exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"ht!tp://bad_uri\\\",\\\"type\\\":\\\"object\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "Throws IllegalArgumentException (or JsonSchemaException) because the $schema URI is syntactically invalid and cannot be normalized.",
            "description": "The schema node contains a $schema property with a malformed URI. The factory should detect the invalid URI during meta‑schema resolution and raise an exception."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-99/schema#\\\",\\\"type\\\":\\\"object\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "Throws IllegalArgumentException (or JsonSchemaException) because the $schema IRI does not correspond to any supported meta‑schema version.",
            "description": "The $schema value points to an unknown/unsupported draft version. The factory should fail fast when it cannot locate a matching JsonMetaSchema."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":null,\\\"type\\\":\\\"object\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "Throws NullPointerException (or IllegalArgumentException) because the $schema field is present but its value is null.",
            "description": "A schema node explicitly defines $schema with a null value. The factory should treat this as an invalid meta‑schema reference and raise an exception."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"   \\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "Throws IllegalArgumentException (or JsonSchemaException) because the $schema string is blank/whitespace only, which cannot be resolved to a meta‑schema.",
            "description": "The $schema property is present but contains only whitespace. The factory should reject this as an invalid meta‑schema identifier."
        }
    ]
}