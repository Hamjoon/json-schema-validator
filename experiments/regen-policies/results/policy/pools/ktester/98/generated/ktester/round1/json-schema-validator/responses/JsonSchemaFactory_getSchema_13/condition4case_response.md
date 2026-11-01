{
    "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "ByteArrayInputStream containing a minimal valid JSON schema: {\"type\":\"object\"}"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "A non‑null JsonSchema object is returned (the method completes without throwing).",
            "description": "Happy‑path: the InputStream can be read, readTree returns a JsonNode and newJsonSchema creates a JsonSchema instance. This verifies the normal execution branch of the try block."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "A custom InputStream whose read method throws IOException (e.g., new InputStream() { public int read() throws IOException { throw new IOException(\"forced\"); } })"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchemaException is thrown and its cause is the original IOException.",
            "description": "Error‑path: readTree propagates an IOException which is caught, logged and re‑thrown as JsonSchemaException. This tests the catch block."
        }
    ]
}