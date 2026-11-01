{
    "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "validJsonSchemaStream (contains a minimal valid JSON schema, e.g. {\"type\":\"object\"})"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance whose meta‑schema is the default (draft‑7) and which can validate a simple JSON object",
            "description": "Normal path – a valid JSON schema is supplied together with an explicit non‑null configuration. The method should forward to the overload with InputFormat.JSON and produce a usable JsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "null"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "throws IllegalArgumentException (or NullPointerException) because the InputStream argument is null",
            "description": "Branch where the required schema InputStream is missing. The factory should reject a null stream early."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "validJsonSchemaStream (same as case 1)"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance using the factory's default SchemaValidatorsConfig",
            "description": "When the configuration argument is null the method should fall back to the default configuration created by createSchemaValidatorsConfig()."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "emptyInputStream (0 bytes)"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "throws IOException (or JsonSchemaException) because the content cannot be parsed into a JsonNode",
            "description": "Edge case where the stream contains no data. The underlying readTree call fails, propagating an exception."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "malformedJsonStream (e.g. \"{ type: object\" )"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "throws com.fasterxml.jackson.core.JsonParseException (wrapped in a JsonSchemaException)",
            "description": "Invalid JSON content should cause the JSON parser to raise a parsing exception, which the factory propagates."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "validYamlSchemaStream (e.g. \"type: object\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "throws com.fasterxml.jackson.core.JsonParseException (or JsonSchemaException) because the method forces InputFormat.JSON while the content is YAML",
            "description": "Even though the stream holds a correct schema in YAML format, the overload used by this method assumes JSON, leading to a parsing failure."
        }
    ]
}