{
    "group": "getSchema(SchemaLocation) - basic functionality",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "classpath:/schemas/simple-schema.json"
                }
            ],
            "expected": "Returns a non‑null JsonSchema instance whose meta‑schema version matches the $schema keyword in the file.",
            "description": "Loads a valid JSON schema from the classpath. This exercises the normal path where the schema is found, parsed and cached."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "file:/tmp/nonexistent-schema.json"
                }
            ],
            "expected": "Throws a JsonSchemaException (or IOException) indicating that the schema cannot be found.",
            "description": "Supplies a SchemaLocation that points to a non‑existent resource. Triggers the branch that handles missing resources."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": null
                }
            ],
            "expected": "Throws NullPointerException (or IllegalArgumentException) because the argument is null.",
            "description": "Verifies that the method validates its input and does not accept a null SchemaLocation."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "classpath:/schemas/invalid-json-schema.json"
                }
            ],
            "expected": "Throws a JsonSchemaException (or JsonProcessingException) because the content cannot be parsed as valid JSON/YAML.",
            "description": "Provides a schema file that exists but contains malformed JSON/YAML, exercising the error‑handling branch for parsing failures."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "classpath:/schemas/schema-with-remote-ref.json"
                }
            ],
            "expected": "Returns a JsonSchema whose child schema references are resolved using the default SchemaLoader.",
            "description": "Tests the branch where the loaded schema contains `$ref` to external schemas, ensuring the loader is invoked."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "classpath:/schemas/schema-with-meta-iri.json"
                }
            ],
            "expected": "Returns a JsonSchema whose meta‑schema is loaded via loadMetaSchema() based on the IRI declared in the schema.",
            "description": "Covers the conditional that checks for an explicit meta‑schema IRI and loads it."
        }
    ]
}