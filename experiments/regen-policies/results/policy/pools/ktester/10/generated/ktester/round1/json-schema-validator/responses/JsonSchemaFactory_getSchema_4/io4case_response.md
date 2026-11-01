{
    "groups": [
        {
            "group": "getSchema(SchemaLocation) - HTTP resource",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://localhost:8080/schemas/http-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the schema is loaded over HTTP without throwing an exception",
                    "description": "Verify that the factory can resolve a schema located at an HTTP URL (using a lightweight test server) and produce a valid JsonSchema object."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - unsupported scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"ftp://example.com/schemas/unsupported-schema.json\")"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown because the URI scheme is not supported by JsonSchemaFactory",
                    "description": "Ensure that a schema location with an unsupported protocol (e.g., ftp) fails fast with a clear exception."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - empty location string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"\")"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) is thrown indicating that the location string is empty or invalid",
                    "description": "Check that providing an empty string to SchemaLocation results in a validation error rather than attempting to load a schema."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - $ref disabled via config",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"classpath:/schemas/schema-with-$ref.json\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig().setHandleReference(false)"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) is thrown because $ref handling is disabled but the schema contains a $ref",
                    "description": "Validate that when the SchemaValidatorsConfig disables $ref processing, loading a schema that contains $ref results in an exception, confirming the config is respected."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - YAML schema parsing",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"classpath:/schemas/yaml-schema.yaml\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the YAML content is correctly parsed into a JSON schema model",
                    "description": "Test that the factory detects a .yaml/.yml file, uses the YAML mapper, and successfully creates a JsonSchema object."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - custom meta‑schema id",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"classpath:/schemas/custom-meta-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned and its meta‑schema version matches the custom meta‑schema id defined inside the schema (e.g., \"https://example.com/custom-meta-schema\")",
                    "description": "Ensure that when a schema declares a non‑default $id/meta‑schema URI, the factory loads and applies that meta‑schema rather than falling back to the default."
                }
            ]
        }
    ]
}