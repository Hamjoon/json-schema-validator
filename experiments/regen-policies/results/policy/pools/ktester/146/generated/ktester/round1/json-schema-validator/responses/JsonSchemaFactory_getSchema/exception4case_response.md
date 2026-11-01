{
    "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"$schema\": \"http://json-schema.org/draft-99/schema#\", \"type\": \"object\" }"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchemaException indicating that the `$schema` URI refers to an unknown or unsupported meta‑schema version",
            "description": "The `$schema` keyword points to a draft version that the factory does not recognise; loading should fail with a clear exception."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "IllegalArgumentException (or URISyntaxException) caused by an invalid URI in `$id`",
            "description": "A malformed URI in the `$id` keyword should be detected early and result in an exception rather than an undefined behavior."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "[ { \"type\": \"object\" } ]"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchemaException because the top‑level schema is a JSON array; only an object or boolean is allowed",
            "description": "The JSON Schema specification requires the root schema to be an object or a boolean. Supplying an array must be rejected."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"$ref\": \"http://example.com/not-a-schema.json\" }"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchemaException indicating that the referenced document is not a valid JSON Schema",
            "description": "An external `$ref` that resolves to a JSON document which is not a schema should cause loading to fail."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"$ref\": \"#/invalid~\" }"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchemaException caused by a malformed JSON‑Pointer fragment in the `$ref` value",
            "description": "The `$ref` contains an illegal escape sequence (`~` not followed by `0` or `1`), which must be detected and reported."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "   \\t\\n  "
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "IllegalArgumentException stating that the schema string is blank or contains only whitespace",
            "description": "A string that consists solely of whitespace cannot be parsed as a JSON Schema and should be rejected up‑front."
        }
    ]
}