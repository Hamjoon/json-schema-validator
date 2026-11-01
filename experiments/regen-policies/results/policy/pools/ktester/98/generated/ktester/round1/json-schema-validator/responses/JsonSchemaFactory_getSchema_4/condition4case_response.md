{
    "group": "JsonSchemaFactory.getSchema(SchemaLocation) - basic and edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"classpath:/schemas/valid-schema.json\")"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance whose meta‑schema version matches the schema content",
            "description": "Happy‑path: a valid classpath URI points to an existing JSON Schema file; the factory should load the file, parse it and return a usable JsonSchema object."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
                }
            ],
            "expected": "throws NullPointerException (or IllegalArgumentException) because the supplied SchemaLocation is null",
            "description": "Validate that the method guards against a null argument and fails fast."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
                }
            ],
            "expected": "throws IllegalArgumentException indicating an unsupported URI scheme",
            "description": "The factory only supports http, https, classpath and file schemes; an unsupported scheme should cause an explicit exception."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"classpath:/schemas/non‑existent.json\")"
                }
            ],
            "expected": "throws JsonSchemaException (or IOException) because the resource cannot be found",
            "description": "When the location points to a resource that does not exist, the loader must surface the error."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"http://json-schema.org/draft-07/schema#\")"
                }
            ],
            "expected": "returns a JsonSchema whose meta‑schema IRI is \"http://json-schema.org/draft-07/schema#\"",
            "description": "Meta‑schema URIs are treated specially; the factory should recognise the IRI and return a JsonSchema that represents the draft‑07 meta‑schema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"classpath:/schemas/relative/../base-schema.json\")"
                }
            ],
            "expected": "returns a non‑null JsonSchema that corresponds to the normalized path (i.e., classpath:/schemas/base-schema.json)",
            "description": "The method should normalise relative path components (\"..\", \".\") before loading the schema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"file:/tmp/empty-schema.json\")"
                }
            ],
            "expected": "throws JsonSchemaException because the file exists but does not contain a valid JSON Schema document",
            "description": "Even if the file is reachable, an empty or malformed JSON document must be detected and reported."
        }
    ]
}