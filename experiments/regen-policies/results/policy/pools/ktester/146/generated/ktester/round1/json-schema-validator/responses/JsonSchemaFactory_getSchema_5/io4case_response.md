{
    "group": "JsonSchemaFactory.getSchema(String) - additional cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "   "
                }
            ],
            "expected": "IllegalArgumentException because the string contains only whitespace and cannot be resolved to a schema location",
            "description": "Verify that a schema string consisting solely of whitespace is rejected early, similar to an empty string."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "http:/example.com/malformed-uri"
                }
            ],
            "expected": "IllegalArgumentException (or URISyntaxException wrapped) because the URI syntax is invalid",
            "description": "Pass a malformed URI (missing second slash after the scheme) to ensure the method validates URI syntax before attempting to load."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "classpath:/schemas/missing-schema.json"
                }
            ],
            "expected": "IllegalArgumentException (or SchemaNotFoundException) because the class‑path resource does not exist",
            "description": "Request a class‑path resource that cannot be found; the factory should fail fast with a clear exception."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "file:/nonexistent/path/schema.json"
                }
            ],
            "expected": "IOException (or IllegalArgumentException) because the file cannot be read",
            "description": "Provide a file‑URI that points to a non‑existent file to test error handling for inaccessible file resources."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{}"
                }
            ],
            "expected": "non‑null JsonSchema instance representing an empty schema (valid according to Draft‑07 meta‑schema)",
            "description": "Supply the minimal valid JSON schema – an empty object – and verify that the factory returns a schema object without errors."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "[{\"type\":\"object\"}]"
                }
            ],
            "expected": "JsonSchemaCreationException (or ValidationException) because the root node is an array, not an object",
            "description": "Pass a JSON array as the top‑level schema node; the method should reject it as an invalid schema structure."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "\uFEFF{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
                }
            ],
            "expected": "non‑null JsonSchema instance; the leading UTF‑8 BOM is ignored and the schema is parsed successfully",
            "description": "Test that a schema string beginning with a Unicode BOM is handled correctly by the parser."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"$ref\": \"#/definitions/foo\", \"definitions\": { \"foo\": { \"type\": \"string\" } } }"
                }
            ],
            "expected": "non‑null JsonSchema instance with a resolved $ref to an internal definition",
            "description": "Provide a schema that uses $ref to reference an internal definition; the factory must resolve the reference and produce a valid JsonSchema."
        }
    ]
}